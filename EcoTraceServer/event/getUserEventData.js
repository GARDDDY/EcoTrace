const express = require('express');
// const { checkOAuth2 } = require('../tech/oauth');
// const { getRules } = require('../tech/getUserRules');
// const { areUsersFriends } = require('../tech/areUsersFriends');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.get('/getUserEventData', async (req, res) => {
    const userId = req.query.cid || '0';
    const eventId = req.query.eventId || '0';
    const oAuth = req.query.oauth || '0';

    if (!connection2 || !connection1) {
        console.error("not connected to events or users")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    // if (!await checkOAuth2(oAuth, requestFrom)) {
    //     return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    // }

    const data = {};

    const [event] = await connection2.execute(
        `SELECT * FROM event WHERE eventId = "${eventId}"`
    );

    data.eventInfo = event[0];
    

    const [inEvent] = await connection1.execute(
        `SELECT * FROM events WHERE eventId = "${eventId}" AND userId = "${userId}"`
    );

    data.isUserInEvent = true//inEvent.length > 0;

    data.isUserCreator = data.eventInfo.eventCreatorId === userId;

    res.json(data);
});

module.exports = router;
