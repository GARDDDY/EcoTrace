const logger = require('../logger');
const { one, query } = require('../core/db');
const { CALCULATORS, KS, CARBON_UNIT, SPECIFY, SPECIFY_AVAILABLE, MSK_TODAY, mskTodayString } = require('./shared');

const GROQ_URL = 'https://api.groq.com/openai/v1/chat/completions';
const PERIODS = [7, 30];
const MAX_LISTED_GAPS = 10;

function periodDates(days) {
    const base = new Date(`${mskTodayString()}T00:00:00Z`);
    const dates = [];
    for (let d = days; d >= 1; --d) {
        const day = new Date(base);
        day.setUTCDate(base.getUTCDate() - d);
        dates.push(day.toISOString().slice(0, 10));
    }
    return dates;
}

async function fetchSpecifyBreakdown(userId, calcId, days) {
    const questions = SPECIFY_AVAILABLE
        .map((s) => s.split('_').map(Number))
        .filter(([c]) => c === Number(calcId))
        .map(([, q]) => q);

    const parts = [];
    for (const question of questions) {
        const rows = await query(
            `SELECT s.key AS specify_key, SUM(s.value::numeric) AS total
             FROM calc_entry, jsonb_each_text(answers->$3->'specify') AS s(key, value)
             WHERE user_id = $1 AND calculator_id = $2
               AND entry_date < ${MSK_TODAY} AND entry_date >= ${MSK_TODAY} - ${days}
               AND answers->$3 ? 'specify'
             GROUP BY s.key ORDER BY total DESC`,
            [userId, calcId, String(question)]
        );
        if (rows.length === 0) continue;
        const names = SPECIFY[`${calcId}${question}`] || {};
        const line = rows.map((r) => `${names[r.specify_key]?.name || `#${r.specify_key}`}: ${Number(r.total).toFixed(0)} г`).join(', ');
        parts.push(line);
    }
    return parts.join('; ') || null;
}

function buildPrompt(calcName, days, unit, rows, missingDates, specifyBreakdown) {
    const dataLines = rows
        .map((r) => `${r.entry_date}: ${r.total.toFixed(2)} ${unit}`)
        .join('\n');

    const span = days === 7 ? 'последние 7 дней' : 'последние 30 дней';
    const gapsCount = missingDates.length;
    const gapsBlock = gapsCount === 0
        ? 'Данные внесены за каждый день периода.'
        : gapsCount <= MAX_LISTED_GAPS
            ? `Данные НЕ вносились ${gapsCount} из ${days} дней периода — пропущенные даты: ${missingDates.join(', ')}.`
            : `Данные НЕ вносились ${gapsCount} из ${days} дней периода (пропусков больше, чем внесённых дней).`;

    const specifyBlock = specifyBreakdown
        ? `Уточняющая разбивка по категориям за этот период (в граммах): ${specifyBreakdown}.`
        : '';

    return `Ты анализируешь дневник пользователя экологического приложения по калькулятору "${calcName}" `
        + `(это часть учёта его выбросов CO2) за ${span}.\n\n`
        + `Фактические данные — дата: количество ${unit} за этот день:\n${dataLines || '(записей нет)'}\n\n`
        + `${gapsBlock}\n`
        + `${specifyBlock}\n\n`
        + `Правила:\n`
        + `1. Используй ТОЛЬКО цифры, даты и категории из данных выше — не придумывай и не округляй в другие `
        + `числа, которых там нет.\n`
        + `2. Обязательно упомяни хотя бы одну конкретную дату и её значение из данных как подтверждение своего `
        + `вывода (особенно если есть заметный пик, спад, или пропуски дней — если пользователь давно не вносил `
        + `данные, прямо скажи об этом).\n`
        + `3. Если есть уточняющая разбивка по категориям — используй её, чтобы дать предметный совет `
        + `(например, какую категорию стоит сократить).\n`
        + `4. Пиши на русском, на "вы", 2-3 предложения, без вступлений, приветствий и извинений: назови тренд `
        + `(рост, снижение, стабильность) именно за ${span} и дай один краткий практический совет по снижению `
        + `выбросов CO2 с учётом этого тренда.`;
}

async function generatePeriodSummary(userId, calcId, days) {
    const rawRows = await query(
        `SELECT entry_date::text AS entry_date, SUM((val->>'value')::numeric) AS total
         FROM calc_entry, jsonb_each(answers) AS e(key, val)
         WHERE user_id = $1 AND calculator_id = $2
           AND entry_date < ${MSK_TODAY} AND entry_date >= ${MSK_TODAY} - ${days}
         GROUP BY entry_date ORDER BY entry_date`,
        [userId, calcId]
    );
    if (rawRows.length === 0) return null;

    const rows = rawRows.map((r) => ({ entry_date: r.entry_date, total: Number(r.total) * KS[calcId] }));
    const present = new Set(rows.map((r) => r.entry_date));
    const missingDates = periodDates(days).filter((d) => !present.has(d));
    const specifyBreakdown = await fetchSpecifyBreakdown(userId, calcId, days);

    const calcName = CALCULATORS[calcId] ?? `Калькулятор ${calcId}`;
    const response = await fetch(GROQ_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${process.env.GROQ_API_KEY}`,
        },
        body: JSON.stringify({
            model: process.env.GROQ_MODEL || 'llama-3.3-70b-versatile',
            messages: [{ role: 'user', content: buildPrompt(calcName, days, CARBON_UNIT, rows, missingDates, specifyBreakdown) }],
            temperature: 0.4,
            max_tokens: 220,
        }),
    });

    if (!response.ok) {
        logger.error('Groq request failed', { status: response.status, body: await response.text() });
        return null;
    }

    const data = await response.json();
    const text = data.choices?.[0]?.message?.content?.trim();
    if (!text) return null;

    return one(
        `INSERT INTO calc_ai_summary (user_id, calculator_id, period_days, summary, generated_for)
         VALUES ($1, $2, $3, $4, ${MSK_TODAY})
         ON CONFLICT (user_id, calculator_id, period_days)
         DO UPDATE SET summary = EXCLUDED.summary, generated_for = EXCLUDED.generated_for, created_at = now()
         RETURNING summary, generated_for::text AS generated_for`,
        [userId, calcId, days, text]
    );
}

async function ensureFreshSummaries(userId, calcId) {
    if (!process.env.GROQ_API_KEY) return PERIODS.map((period) => ({ period, summary: null, generatedFor: null }));

    const existingRows = await query(
        `SELECT period_days, summary, generated_for::text AS generated_for
         FROM calc_ai_summary WHERE user_id = $1 AND calculator_id = $2`,
        [userId, calcId]
    );
    const byPeriod = new Map(existingRows.map((r) => [r.period_days, r]));
    const today = mskTodayString();

    return Promise.all(PERIODS.map(async (period) => {
        const existing = byPeriod.get(period);
        if (existing && existing.generated_for === today) {
            return { period, summary: existing.summary, generatedFor: existing.generated_for };
        }
        const fresh = await generatePeriodSummary(userId, calcId, period).catch((e) => {
            logger.error('Groq summary generation failed', { err: e.message, period });
            return null;
        });
        if (fresh) return { period, summary: fresh.summary, generatedFor: fresh.generated_for };
        return { period, summary: existing?.summary ?? null, generatedFor: existing?.generated_for ?? null };
    }));
}

module.exports = { ensureFreshSummaries };
