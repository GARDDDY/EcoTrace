const express = require('express');

const connections = require("../server")
const connection2 = connections["events"]

const router = express.Router();

router.get('/getAllEvents', async (req, res) => {
    const filters = req.query.filters || "";
    var startAfter = req.query.nei || null;
    if (startAfter == "null") startAfter = null;

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {
        let query = 'SELECT * FROM event WHERE filters LIKE ?';
        let params = [`%${filters}%`];

        if (startAfter !== null) {
            query += ' AND eventId > ?';
            params.push(startAfter);
        }
        query += ' LIMIT 3'

        const [events] = await connection2.execute(query, params);

        let countQuery = 'SELECT COUNT(*) as total FROM event WHERE filters LIKE ?';
        let countParams = [`%${filters}%`];

        if (startAfter !== null) {
            countQuery += ' AND eventId > ?';
            countParams.push(startAfter);
        }

        const [countResult] = await connection2.execute(countQuery, countParams);
        const totalRecords = countResult[0].total;

        const hasMore = events.length === 3 && totalRecords > 3;

        res.json([!hasMore, events]);

    } catch (error) {
        console.error('Error fetching event details:', error);
        res.status(500).json({ error: 'Error fetching event details' });
    }
});

module.exports = router;
