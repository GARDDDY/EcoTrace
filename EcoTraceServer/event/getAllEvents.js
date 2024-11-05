const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

function formatDate(timeString) {
    const date = new Date(timeString*1000);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}.${month}.${year}`;
}

router.get('/getAllEvents', async (req, res) => {
    const filters = req.query.filters || "";
    const startAfter = req.query.nei === "null" ? null : req.query.nei;

    var start = req.query.s || null;
    if (start === "null") start = null;
    var end = req.query.e || null;
    if (end === "null") end = null;

    if (!connection2) {
        console.error("not connected to events");
        return;
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {
        let query = `
            SELECT e.*, 
                (SELECT MIN(SUBSTRING_INDEX(timeId, '_', 1)) FROM events.eventtimes et WHERE et.eventId = e.eventId) AS startTime,
                (SELECT MAX(SUBSTRING_INDEX(timeId, '_', -1)) FROM events.eventtimes et WHERE et.eventId = e.eventId) AS endTime
            FROM event e 
            WHERE e.filters LIKE ?`;

        let params = [`%${filters}%`];

        if (startAfter !== null) {
            query += ' AND e.eventId > ?';
            params.push(startAfter);
        }

        if (start !== null) {
            query += ' AND (SELECT MIN(SUBSTRING_INDEX(timeId, \'_\', 1)) FROM events.eventtimes et WHERE et.eventId = e.eventId) >= ?';
            params.push(start);
        }

        if (end !== null) {
            query += `
                AND (SELECT MIN(SUBSTRING_INDEX(timeId, '_', 1)) FROM events.eventtimes et WHERE et.eventId = e.eventId) <= ?
                AND (SELECT MAX(SUBSTRING_INDEX(timeId, '_', -1)) FROM events.eventtimes et WHERE et.eventId = e.eventId) >= ?`;
            
            params.push(end, start);
        } else if (end === null && start !== null) {
            // Если end равно null, проверяем только на start
            query += `
                AND (SELECT MAX(SUBSTRING_INDEX(timeId, '_', -1)) FROM events.eventtimes et WHERE et.eventId = e.eventId) >= ?`;
            params.push(start);
        }

        query += ' LIMIT 3';

        const [events] = await connection2.execute(query, params);

        console.log("TIME SORTED", events)

        let countQuery = 'SELECT COUNT(*) as total FROM event e WHERE e.filters LIKE ?';
        let countParams = [`%${filters}%`];

        if (startAfter !== null) {
            countQuery += ' AND e.eventId > ?';
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

        const currentTime = Date.now()/1000;
        const formattedEvents = events.map(event => ({
            ...event,
            eventStatus: event.startTime <= currentTime && currentTime <= event.endTime && 1 || currentTime < event.startTime && 0 || 2,
            eventStatusString: event.startTime <= currentTime && currentTime <= event.endTime && "Проходит" ||
            currentTime < event.startTime && `Начнется ${formatDate(event.startTime)}` ||
            `Закончилось ${formatDate(event.endTime)}`,
            eventCreatorName: usernames[event.eventCreatorId] || null
        }));

        res.json([!hasMore, formattedEvents]);

    } catch (error) {
        console.error('Error fetching event details:', error);
        res.status(500).json({ error: 'Error fetching event details' });
    }
});

module.exports = router;


