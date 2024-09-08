const express = require('express');

const connections = require("../server")
const connection2 = connections["events"]

const router = express.Router();

router.get('/getEventGoals', async (req, res) => {
    const eventId = req.query.eventId || '0';

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const data = []

    const [goals] = await connection2.execute(
        `SELECT goalText FROM eventgoals WHERE eventId = "${eventId}"`
    );
    goals.forEach(element => {
        data.push(element.goalText)
    });

    res.json(data);
});

module.exports = router;
