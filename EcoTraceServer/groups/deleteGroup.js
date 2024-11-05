const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();

router.get('/deleteGroup', async (req, res) => {
    const groupId = req.query.gid || "0";
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

        const [isUserAnOwner] = await connection2.execute('SELECT groupCreatorId FROM `group` WHERE groupId = ?', [groupId]);

        if (isUserAnOwner[0].groupCreatorId !== userId) {
            return res.send([false])
        }


        await connection2.execute("delete from `group` where groupId = ?", [groupId])
        await connection2.execute("delete from posts where groupId = ?", [groupId])
        await connection2.execute("delete from comments where groupId = ?", [groupId])
        await connection1.execute("delete from `groups` where groupId = ?", [groupId])

        return res.send([true])
});

module.exports = router;
