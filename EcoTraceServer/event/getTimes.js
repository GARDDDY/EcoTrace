const express = require('express');

const connections = require("../server")
const connection2 = connections["events"]

const router = express.Router();

router.get('/getEventTimes', async (req, res) => {
    const eventId = req.query.eventId || '0';

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const data = {}

    const [times] = await connection2.execute(
        `SELECT timeId, timeName FROM eventtimes WHERE eventId = "${eventId}"`
    );
    times.forEach(element => {
        data[element.timeId] = element.timeName
    });

    res.json(data);
});

module.exports = router;
