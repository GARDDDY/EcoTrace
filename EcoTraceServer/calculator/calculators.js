const express = require('express');
const { z } = require('zod');
const { route, HttpError } = require('../core/route');
const { validate } = require('../core/validate');
const { query, one, tx } = require('../core/db');
const { MSK_TODAY } = require('./shared');
const { ensureFreshSummaries } = require('./groq');

const router = express.Router();
const CALC_REWARD = 2;

// GET /calculators -- справочник калькуляторов с вопросами.
router.get('/calculators', route(async () => {
    return query(
        `SELECT c.id, c.name, c.position,
                COALESCE(json_agg(json_build_object('id', q.id, 'text', q.text, 'position', q.position)
                         ORDER BY q.position) FILTER (WHERE q.id IS NOT NULL), '[]') AS questions
         FROM calculator c
         LEFT JOIN calculator_question q ON q.calculator_id = c.id
         GROUP BY c.id
         ORDER BY c.position`
    );
}, { auth: false }));

// GET /calculators/:id/state -- средние значения пользователя + вносил ли он сегодня.
router.get('/calculators/:id/state', route(async (req) => {
    const calcId = req.params.id;

    const meta = await one(
        `SELECT MAX(entry_date)               AS last_date,
                ${MSK_TODAY} - MAX(entry_date) AS days_since,
                BOOL_OR(entry_date = ${MSK_TODAY}) AS today,
                COUNT(*)::int                 AS total
         FROM calc_entry WHERE user_id = $1 AND calculator_id = $2`,
        [req.user.id, calcId]
    );

    const averages = await query(
        `SELECT key::int AS question, AVG((val->>'value')::numeric) AS value, COUNT(*)::int AS count
         FROM calc_entry, jsonb_each(answers) AS e(key, val)
         WHERE user_id = $1 AND calculator_id = $2
         GROUP BY key ORDER BY key`,
        [req.user.id, calcId]
    );

    const todayTotals = await query(
        `SELECT key::int AS question, SUM((val->>'value')::numeric) AS value
         FROM calc_entry, jsonb_each(answers) AS e(key, val)
         WHERE user_id = $1 AND calculator_id = $2 AND entry_date = ${MSK_TODAY}
         GROUP BY key ORDER BY key`,
        [req.user.id, calcId]
    );
    const todaySpecify = await query(
        `SELECT (e.key)::int AS question, s.key AS specify_key, SUM(s.value::numeric) AS value
         FROM calc_entry, jsonb_each(answers) AS e(key, val), jsonb_each_text(val -> 'specify') AS s(key, value)
         WHERE user_id = $1 AND calculator_id = $2 AND entry_date = ${MSK_TODAY} AND val ? 'specify'
         GROUP BY e.key, s.key`,
        [req.user.id, calcId]
    );
    const specifyByQuestion = {};
    todaySpecify.forEach((r) => {
        (specifyByQuestion[r.question] ||= {})[r.specify_key] = Number(r.value);
    });

    return {
        updating: meta.today ?? false,
        first: (meta.total ?? 0) === 0,
        days_since: meta.days_since ?? null,
        averages: averages.map((a) => ({ question: a.question, value: Number(a.value), count: a.count })),
        today: todayTotals.map((t) => ({
            question: t.question,
            value: Number(t.value),
            specify: specifyByQuestion[t.question] ?? null,
        })),
    };
}));

const entrySchema = z.object({
    answers: z.array(z.object({
        value: z.number(),
        specify: z.record(z.string(), z.number()).optional(),
    })).min(1),
});

// POST /calculators/:id/entries -- внести данные. Несколько раз в день суммируется (новая строка).

router.post('/calculators/:id/entries', route(async (req) => {
    const calcId = req.params.id;
    const { answers } = validate(entrySchema, req.body);

    if (!(await one('SELECT 1 FROM calculator WHERE id = $1', [calcId]))) {
        throw new HttpError(404, 'calculator not found');
    }

    const answersJson = {};
    answers.forEach((a, i) => {
        const total = a.specify ? Object.values(a.specify).reduce((s, v) => s + v, 0) : a.value;
        answersJson[i] = a.specify ? { value: total, specify: a.specify } : { value: total };
    });

    const result = await tx(async (client) => {
        const { rows: [pre] } = await client.query(
            `SELECT EXISTS(
                SELECT 1 FROM calc_entry
                WHERE user_id = $1 AND calculator_id = $2 AND entry_date = ${MSK_TODAY}
             ) AS today`,
            [req.user.id, calcId]
        );

        await client.query(
            `INSERT INTO calc_entry (user_id, calculator_id, entry_date, answers)
             VALUES ($1, $2, ${MSK_TODAY}, $3)`,
            [req.user.id, calcId, JSON.stringify(answersJson)]
        );

        if (pre.today) return { awarded: 0, message: 'Данные добавлены к сегодняшним' };

        await client.query(
            `INSERT INTO experience_ledger (user_id, amount, reason, ref_id) VALUES ($1, $2, 'calculator', $3)`,
            [req.user.id, CALC_REWARD, calcId]
        );
        await client.query('UPDATE app_user SET experience = experience + $1 WHERE id = $2',
            [CALC_REWARD, req.user.id]);
        return { awarded: CALC_REWARD, message: `Данные добавлены, +${CALC_REWARD} опыта` };
    });

    return result;
}));

// GET /calculators/:id/ai-summary -- анализы за 7 и 30 дней. Пересчитываются лениво,
// не по расписанию, а при первом за сутки запросе (т.е. когда пользователь открывает советы).
router.get('/calculators/:id/ai-summary', route(async (req) => {
    const items = await ensureFreshSummaries(req.user.id, req.params.id);
    return { items };
}));

module.exports = router;
