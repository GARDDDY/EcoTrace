const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection2 = connections["users"]

const router = express.Router();

router.get('/isUserValidated', async (req, res) => {
    const userId = req.query.cid || '0';
    const oAuth = req.query.oauth || '0';
    const eventId = req.query.eventId || '0';

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    if (!await checkOAuth2(oAuth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    

    const [valid] = await connection2.execute(
        `SELECT validated FROM events WHERE userId = ? and eventId = ?`, [userId, eventId]
    );

    res.send([Boolean(valid[0].validated)]);
});

module.exports = router;
