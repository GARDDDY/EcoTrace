const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.get('/joinEvent', async (req, res) => { // todo set on get
    const userId = req.query.cuid || '0';
    const oauth = req.query.oauth || '0';
    const eventId = req.query.eid || '0';


    if (!connection1) {
        console.error("not connected to users")
        return
    }

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const [rows] = await connection1.execute(
        `SELECT COUNT(*) AS count FROM events WHERE userId = ? AND eventId = ?`,
        [userId, eventId]
    );
    
    const found = rows[0].count > 0;

    if (!found) {

        await connection1.execute(
            `INSERT INTO events (userId, eventId, eventRole, validated) VALUES (?,?,?,?)`,
            [userId, eventId, 2, false]
        );

        await connection2.execute(
            `UPDATE event SET eventCountMembers = eventCountMembers + 1 WHERE eventId = ?`,
            [eventId]
        );
    }

    res.status(200);
});

module.exports = router;