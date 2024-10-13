const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.post('/leaveEvent', async (req, res) => {
    const userId = req.query.cuid || '0';
    const oauth = req.query.oauth || '0';
    const eventId = req.query.eid || '0';


    if (!connection1) {
        console.error("not connected to users")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const [rows] = await connection1.execute(
        `SELECT COUNT(*) AS count FROM events WHERE userId = ? AND eventId = ?`,
        [userId, eventId]
    );
    
    const found = rows[0].count > 0;

    if (found) {

        await connection1.execute(
            `DELETE FROM events WHERE userId = ? AND eventId = ? AND eventRole != 0`,
            [userId, eventId]
        );
        await connection2.execute(
            `UPDATE event SET eventCountMembers = eventCountMembers - 1 WHERE eventId = ?`,
            [eventId]
        );
    }

    res.status(200);
});

module.exports = router;
