const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');
// const { getRules } = require('../tech/getUserRules');
// const { areUsersFriends } = require('../tech/areUsersFriends');

const connections = require("../server");
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

router.get('/getUserEventData', async (req, res) => {
    const userId = req.query.cid || '0';
    const eventId = req.query.eventId || '0';
    const oAuth = req.query.oauth || '0';

    if (!connection2 || !connection1) {
        console.error("not connected to events or users")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oAuth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    const data = {};

    const [eventData] = await connection2.execute(
        `SELECT e.*, 
        (SELECT MIN(SUBSTRING_INDEX(timeId, '_', 1)) FROM eventtimes WHERE eventId = e.eventId) AS minTime, 
        (SELECT MAX(SUBSTRING_INDEX(timeId, '_', -1)) FROM eventtimes WHERE eventId = e.eventId) AS maxTime 
        FROM event e 
        WHERE eventId = "${eventId}"`
    );

    const event = eventData[0];
    const currentTime = Date.now()/1000;
    console.error("time", currentTime)
    event.eventStatus = event.minTime <= currentTime && currentTime <= event.maxTime && 1 || currentTime < event.minTime && 0 || 2;
    event.eventStatusString = event
    ? event.minTime <= currentTime && currentTime <= event.maxTime
        ? `Проходит до `
        : currentTime < event.minTime
        ? `Начнется `
        : `Проходило с `
    : "???";
    const [rows] = await connection1.execute('SELECT COUNT(*) AS total FROM events WHERE eventId = ?', [eventId]);
    event.eventCountMembers = rows[0].total;

    console.log("events", event)

    data.eventInfo = event;
    

    const [inEvent] = await connection1.execute(
        `SELECT * FROM events WHERE eventId = ? AND userId = ?`, [eventId, userId]
    );

    data.isUserInEvent = inEvent.length > 0;

    data.isUserCreator = data.eventInfo.eventCreatorId === userId;

    res.json(data);
});

module.exports = router;
