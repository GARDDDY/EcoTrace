const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.get('/leaveGroup', async (req, res) => {
    const userId = req.query.cid || '0';
    const oauth = req.query.oauth || '0';
    const groupId = req.query.gid || '0';


    if (!connection1) {
        console.error("not connected to users")
        return
    }

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const [rows] = await connection1.execute(
        `SELECT role FROM \`groups\` WHERE userId = ? AND groupId = ?`,
        [userId, groupId]
    );
    
    if (rows.length === 0 || rows[0].role === 0) {
        return res.send([false]);
    }

        await connection1.execute(
            `DELETE FROM \`groups\` WHERE userId = ? AND groupId = ? AND role != 0`,
            [userId, groupId]
        );
    
    res.status(200).send([true]);
});

module.exports = router;
