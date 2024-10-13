const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]
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


        const usersIds = [...new Set(events.map(event => event.eventCreatorId))];
        const userIds = usersIds.map(id => `'${id}'`).join(', ');
        
        let usernames = {};
        
        if (userIds.length > 0) {
            const userQuery = `SELECT userId, username FROM user WHERE userId IN (${userIds})`;
            const [users] = await connection1.execute(userQuery);
            usernames = users.reduce((acc, user) => {
                acc[user.userId] = user.username;
                return acc;
            }, {});
        }
    
        const formattedEvents = events.map(event => ({
            ...event,
            eventCreatorName: usernames[event.eventCreatorId] || null
        }));


        res.json([!hasMore, formattedEvents]);

    } catch (error) {
        console.error('Error fetching event details:', error);
        res.status(500).json({ error: 'Error fetching event details' });
    }
});

module.exports = router;
