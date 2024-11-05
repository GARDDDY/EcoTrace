const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();

router.get('/deleteComment', async (req, res) => {
    const groupId = req.query.gid || "0";
    const postId = req.query.pid || '0';
    const commentId = req.query.comment || '0';
    const userId = req.query.cid || "0";
    const oauth = req.query.oauth || "0";

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    if (!connection2) {
        console.error("not connected to groups")
        return;
    }

    const [userInGroup] = await connection1.execute('SELECT role FROM `groups` WHERE userId = ? and groupId = ?', [userId, groupId]);

    if (userInGroup.length === 0) {
        console.log("not in group")
        return res.status(403).send([false]);
    }

    const [author] = await connection2.execute('select commentCreatorId from comments where groupId = ? and postId = ? and commentId = ?', [groupId, postId, commentId])
    const [authorRole] = await connection1.execute('SELECT role FROM `groups` WHERE userId = ? and groupId = ?', [author[0].commentCreatorId, groupId])

    if (userInGroup[0].role >= authorRole[0].role && author[0].commentCreatorId !== userId) {
        console.log("bad role")
        return res.send([false])
    }

    await connection2.execute('delete from comments where groupId = ? and postId = ? and commentId = ?', [groupId, postId, commentId])

    //todo
    res.send([true])
});

module.exports = router;
