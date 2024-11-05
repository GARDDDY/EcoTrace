const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');
const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.get('/setEventRole', async (req, res) => {
    const userId = req.query.cid || '0';
    const oauth = req.query.oauth || '0';
    const eventId = req.query.eid || '0';
    const user = req.query.uid || '0';
    const role = req.query.role || -1;

    // if (role !== 1 || role !== 2) {
    //     console.log(role)
    //     return res.send([false])
    // }

    if (!connection1) {
        console.error("not connected to users")
        return
    }

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const [userRole] = await connection1.execute(
        `SELECT eventRole FROM events WHERE userId = ? AND eventId = ?`,
        [user, eventId]
    );
    const [userActionRole] = await connection1.execute(
        `SELECT eventRole from events WHERE userId = ? AND eventId = ?`,
        [userId, eventId]
    );
    

    if (userActionRole[0].eventRole === 0 && userRole.length > 0) {
        await connection1.execute(
            `UPDATE events SET eventRole = ? WHERE eventId = ? and userId = ?`,
            [role, eventId, user]
        );

        return res.send([true])
    }

    res.send([false]);
});

module.exports = router;
