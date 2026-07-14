const express = require('express');
const { route } = require('../core/route');
const { query } = require('../core/db');

const router = express.Router();

// GET /rating?country=RU&limit=20&offset=0 -- топ по опыту
const TOP_LIMIT = 200;

router.get('/rating', route(async (req) => {
    const country = req.query.country || null;
    const pageSize = Math.min(Math.max(Number(req.query.limit) || 20, 1), 50);
    const offset = Math.max(Number(req.query.offset) || 0, 0);

    if (offset >= TOP_LIMIT) return { items: [], has_more: false };
    const limit = Math.min(pageSize, TOP_LIMIT - offset);

    const rows = await query(
        `SELECT id, username, experience, avatar_key,
                CASE WHEN country_visibility = 'everyone' THEN country_code ELSE NULL END AS country_code
         FROM app_user
         WHERE ($1::text IS NULL OR (country_code = $1 AND country_visibility = 'everyone'))
         ORDER BY experience DESC, id
         LIMIT $2 OFFSET $3`,
        [country, limit + 1, offset]
    );
    const items = rows.slice(0, limit);
    const has_more = rows.length > limit && (offset + limit) < TOP_LIMIT;
    return { items, has_more };
}, { auth: false }));

module.exports = router;
