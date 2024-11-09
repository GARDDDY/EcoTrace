const express = require('express');
const { route } = require('../core/route');
const { one } = require('../core/db');

const router = express.Router();

// GET /updates?since=<epoch ms> -- счётчики
router.get('/updates', route(async (req) => {
    const since = new Date(Number(req.query.since) || 0);

    const counts = await one(
        `SELECT
            COUNT(*) FILTER (WHERE e.status = 'active')   AS started,
            COUNT(*) FILTER (WHERE e.status = 'finished') AS ended
         FROM event e
         JOIN event_member m ON m.event_id = e.id
         WHERE m.user_id = $1 AND e.status_updated_at > $2`,
        [req.user.id, since]
    );

    return {
        started: counts.started,
        ended: counts.ended
    };
}));

module.exports = router;
