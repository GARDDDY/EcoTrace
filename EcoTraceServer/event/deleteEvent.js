const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.get('/deleteEvent', async (req, res) => {
    const eventId = req.query.eid || "0";
    const userId = req.query.cid || "0";
    const oauth = req.query.oauth || "0";

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    if (!connection2) {
        console.error("not connected to groups")
        return;
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

        const [isUserAnOwner] = await connection2.execute('SELECT eventCreatorId FROM event WHERE eventId = ?', [eventId]);

        if (isUserAnOwner[0].eventCreatorId !== userId) {
            return res.send([false])
        }


        await connection2.execute("DELETE FROM eventtimes WHERE eventId = ?", [eventId]);
        await connection2.execute("DELETE FROM eventgoals WHERE eventId = ?", [eventId]);
        await connection2.execute("DELETE FROM eventcoords WHERE eventId = ?", [eventId]);
        await connection2.execute("DELETE FROM event WHERE eventId = ?", [eventId]);
        await connection1.execute("DELETE FROM events WHERE eventId = ?", [eventId]);


        return res.send([true])
});

module.exports = router;
