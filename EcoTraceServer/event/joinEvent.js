const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.get('/joinEvent', async (req, res) => {
    const userId = req.query.cid || '0';
    const oauth = req.query.oauth || '0';
    const eventId = req.query.eventId || '0';


    if (!connection1) {
        console.error("not connected to users")
        return
    }

    if (!await checkOAuth2(oauth, userId)) {
        console.log("sign")
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    console.log("1")
    const [rows] = await connection1.execute(
        `SELECT COUNT(*) AS count FROM events WHERE userId = ? AND eventId = ?`,
        [userId, eventId]
    );
    console.log("2")
    const found = rows[0].count > 0;
    console.log("3")
    if (!found) {
        console.log("4")
        await connection1.execute(
            `INSERT INTO events (userId, eventId, eventRole, validated) VALUES (?,?,?,?)`,
            [userId, eventId, 2, false]
        );
        console.log("5")
        await connection2.execute(
            `UPDATE event SET eventCountMembers = eventCountMembers + 1 WHERE eventId = ?`,
            [eventId]
        );
        console.log("6")

        return res.send([true])
    }

    res.send([false]);
});

module.exports = router;
