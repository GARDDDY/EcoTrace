const express = require('express');
const { route } = require('../core/route');
const { query, one } = require('../core/db');
const { CALCULATORS, KS, CARBON_UNIT, CALC_COLORS, SPECIFY, SPECIFY_AVAILABLE, MSK_TODAY, lastDays } = require('./shared');

const router = express.Router();

// GET /calculators/specify-config -- какие (калькулятор_вопрос) имеют уточнения.
router.get('/calculators/specify-config', route(async () => {
    return { available: SPECIFY_AVAILABLE };
}, { auth: false }));

// GET /users/:id/graph?time=0|1&types=0,2 -- углеродный след по дням, серия на калькулятор.
router.get('/users/:id/graph', route(async (req) => {
    const userId = req.params.id;
    const numDays = req.query.time === '1' ? 30 : 7;
    const exclude = req.query.types ? String(req.query.types).split(',').map(Number) : [];

    const rows = await query(
        `SELECT calculator_id, entry_date::text AS entry_date,
                SUM((SELECT SUM((v->>'value')::numeric) FROM jsonb_each(answers) e(k, v))) AS total
         FROM calc_entry
         WHERE user_id = $1 AND entry_date > ${MSK_TODAY} - $2::int
         GROUP BY calculator_id, entry_date`,
        [userId, numDays]
    );

    const { keys, labels } = lastDays(numDays);
    const byCalc = {};
    rows.forEach((r) => { (byCalc[r.calculator_id] ||= {})[r.entry_date] = Number(r.total); });

    const datasets = [];
    for (let c = 0; c < CALCULATORS.length; ++c) {
        if (exclude.includes(c) || !byCalc[c]) continue;
        const data = keys.map((k) => (byCalc[c][k] != null ? +(byCalc[c][k] * KS[c]).toFixed(4) : null));
        if (data.every((v) => v == null)) continue;
        datasets.push({ calculator: c, name: CALCULATORS[c], color: CALC_COLORS[c], data });
    }

    return { unit: CARBON_UNIT, days: keys, labels, datasets };
}, { auth: false }));

// GET /calculators/:id/specify?question=0 -- разбивка уточнений за неделю, серия на вид (свои данные).
router.get('/calculators/:id/specify', route(async (req) => {
    const calcId = req.params.id;
    const question = String(req.query.question || '0');
    const cfg = SPECIFY[`${calcId}${question}`] || {};

    const rows = await query(
        `SELECT entry_date::text AS entry_date, answers->$3->'specify' AS specify
         FROM calc_entry
         WHERE user_id = $1 AND calculator_id = $2
           AND entry_date > ${MSK_TODAY} - 7
           AND answers->$3 ? 'specify'`,
        [req.user.id, calcId, question]
    );

    const { keys, labels } = lastDays(7);
    const series = {};
    rows.forEach((r) => {
        Object.entries(r.specify || {}).forEach(([k, v]) => {
            (series[k] ||= {})[r.entry_date] = (series[k][r.entry_date] || 0) + Number(v);
        });
    });

    const datasets = Object.keys(series).map((k) => ({
        key: Number(k),
        name: cfg[k]?.name || `#${k}`,
        color: cfg[k]?.color || '128, 128, 128',
        data: keys.map((d) => series[k][d] || 0),
    }));

    return { unit: 'граммы', days: keys, labels, datasets };
}));

// GET /calculators/:id/advices?question=0 -- сравнение со средним по своему полу.
router.get('/calculators/:id/advices', route(async (req) => {
    const calcId = req.params.id;
    const question = String(req.query.question || '0');

    const me = await one('SELECT gender FROM app_user WHERE id = $1', [req.user.id]);

    const agg = await one(
        `SELECT COALESCE(SUM(s.total), 0) AS sum_total,
                COUNT(*)::int             AS rows,
                COUNT(DISTINCT ce.user_id)::int AS users
         FROM calc_entry ce
         JOIN app_user u ON u.id = ce.user_id AND u.gender = $2
         CROSS JOIN LATERAL (
            SELECT SUM(v::numeric) AS total
            FROM jsonb_each_text(ce.answers->$3->'specify') AS e(k, v)
         ) s
         WHERE ce.calculator_id = $1 AND ce.answers->$3 ? 'specify'`,
        [calcId, me.gender, question]
    );

    const g = me.gender === 'female' ? 'женщин' : 'мужчин';
    const byRow = agg.rows > 0 ? Number(agg.sum_total) / agg.rows : 0;
    const byUser = agg.users > 0 ? Number(agg.sum_total) / agg.users : 0;

    return [
        `Среднее значение для всех ${g} на сервисе = ${byRow.toFixed(1)} гр.`,
        `Среднее на уникального пользователя (${g}) = ${byUser.toFixed(1)} гр.`,
    ];
}));

module.exports = router;
