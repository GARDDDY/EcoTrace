const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();

router.get('/joinGroup', async (req, res) => { 
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
        `SELECT COUNT(*) AS count FROM \`groups\` WHERE userId = ? AND groupId = ?`,
        [userId, groupId]
    );
    
    const found = rows[0].count > 0;

    if (found) {
        return res.status(200).send([false]);
    }

        await connection1.execute(
            `INSERT INTO \`groups\` (userId, groupId, role) VALUES (?,?,?)`,
            [userId, groupId, 3]
        );
        return res.status(200).send([true]);
    
});

module.exports = router;
