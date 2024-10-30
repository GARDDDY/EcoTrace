const express = require('express');

const connections = require("../server")
const { checkOAuth2 } = require('../tech/oauth');
const connection2 = connections["users"]

const router = express.Router();

router.get('/kickUserFromGroup', async (req, res) => {
    const groupId = req.query.gid || '0';
    const user = req.query.uid || '0';
    const userId = req.query.cid || '0';
    const oauth = req.query.oauth || '0';

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    

    const [valid] = await connection2.execute(
        `delete from \`groups\` where role != 0 and userId = ? and groupId = ?`, [user, groupId]
    );

    res.send([valid.affectedRows > 0]);
});

module.exports = router;
