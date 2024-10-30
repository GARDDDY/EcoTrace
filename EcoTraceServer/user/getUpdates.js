const express = require('express');

const connections = require("../server")
const connection2 = connections["users"]
const connection1 = connections["events"]

const router = express.Router();

router.get('/getUpdates', async (req, res) => {
    const userId = req.query.cid || '0';
    const oAuth = req.query.oauth || '0';
    const sinceTime = req.query.since || 0;

    console.log(sinceTime)

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const [usercall] = await connection2.execute("select username, fullname from user where userId = ?", [userId])
    console.log(usercall)

    const [events] = await connection2.execute(
        `SELECT eventId FROM events WHERE userId = ?`, [userId]
    );

    if (events.length === 0) {
        return res.json({ started: 0, ended: 0 });
    }
    const eventsIds = events.map(event => event.eventId);
    const eventIds = eventsIds.map(id => `'${id}'`).join(', ');

    const [eventsStarted] = await connection1.execute(
        `SELECT COUNT(*) AS count FROM event WHERE eventId IN (${eventIds}) AND statusUpdate > ? AND eventStatus = 1`,
        [sinceTime]
    );

    const [eventsEnded] = await connection1.execute(
        `SELECT COUNT(*) AS count FROM event WHERE eventId IN (${eventIds}) AND statusUpdate > ? AND eventStatus = 2`,
        [sinceTime]
    );

    res.json([eventsStarted[0].count,eventsEnded[0].count, usercall[0].fullname ? usercall[0].fullname : usercall[0].username]);
});

module.exports = router;
