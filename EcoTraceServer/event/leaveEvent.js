const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.get('/leaveEvent', async (req, res) => { // check todo
    const userId = req.query.cid || '0';
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

    const [role] = await connection1.execute(
        `SELECT eventRole AS count FROM events WHERE userId = ? AND eventId = ?`,
        [userId, eventId]
    );

    if (role[0].eventRole === 0 || role.length === 0) {
        return res.send([false])
    }

        await connection1.execute(
            `DELETE FROM events WHERE userId = ? AND eventId = ? AND eventRole != 0`,
            [userId, eventId]
        );
        

    res.status(200).send([true]);
});

module.exports = router;
