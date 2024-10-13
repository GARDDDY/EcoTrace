const express = require('express');

const connections = require("../server")
const connection2 = connections["users"]

const router = express.Router();

router.get('/getAllUsers', async (req, res) => {
    const filters = req.query.filters || "";
    var startAfter = req.query.nei || null;
    var filterName = req.query.name || null;
    if (filterName === "null") filterName = null;
    if (startAfter === "null") startAfter = null;

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {
        let query = 'SELECT userId, filters, username FROM user WHERE filters LIKE ?';
        let params = [`%${filters}%`];

        if (filterName !== null) {
            query += ' AND username LIKE ?';
            params.push(`${filterName}%`);
        }

        if (startAfter !== null) {
            query += ' AND userId > ?';
            params.push(startAfter);
        }
        query += ' LIMIT 3'

        const [users] = await connection2.execute(query, params);

        let countQuery = 'SELECT COUNT(*) as total FROM user WHERE filters LIKE ?';
        let countParams = [`%${filters}%`];

        if (filterName !== null) {
            countQuery += ' AND username LIKE ?';
            countParams.push(`${filterName}%`);
        }

        if (startAfter !== null) {
            countQuery += ' AND userId > ?';
            countParams.push(startAfter);
        }

        const [countResult] = await connection2.execute(countQuery, countParams);
        const totalRecords = countResult[0].total;

        const hasMore = users.length === 3 && totalRecords > 3;

        res.json([!hasMore, users]);

    } catch (error) {
        console.error('Error fetching event details:', error);
        res.status(500).json({ error: 'Error fetching event details' });
    }
});

module.exports = router;
