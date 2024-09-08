const express = require('express');

const connections = require("../server")
const connection2 = connections["users"]

const router = express.Router();

router.get('/isUserModerInEvent', async (req, res) => {
    const userId = req.query.cid || '0';
    const eventId = req.query.eventId || '0';
    const oAuth = req.query.oauth || '0';

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    // if (!await checkOAuth2(oAuth, requestFrom)) {
    //     return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    // }

    const [moder] = await connection2.execute(
        `SELECT eventRole FROM events WHERE eventId = "${eventId}" AND userId = "${userId}"`
    );

    res.json([moder <= 1]);
    // 0 создатель
    // 1 помощник
    // 2 участник
});

module.exports = router;
