const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');
const connections = require("../server");
const connection1 = connections["users"];
const connection2 = connections["groups"];
const router = express.Router();


router.get('/deletePost', async (req, res) => {
    const groupId = req.query.gid || '0';
    const userId = req.query.cid || "0";
    const oauth = req.query.oauth || "0";
    const postId = req.query.pid || '0';

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }


    const [userInGroup] = await connection1.execute('SELECT role FROM `groups` WHERE userId = ? and groupId = ?', [userId, groupId]);

    if (userInGroup.length === 0) {
        return res.status(403).send([false]);
    }

    const [author] = await connection2.execute('select postCreatorId from posts where groupId = ? and postId = ?', [groupId, postId])
    const [authorRole] = await connection1.execute('SELECT role FROM `groups` WHERE userId = ? and groupId = ?', [author[0].postCreatorId, groupId])

    if (userInGroup[0].role >= authorRole[0].role && author[0].postCreatorId !== userId) {
        return res.send([false])
    }

    await connection2.execute('delete from posts where groupId = ? and postId = ?', [groupId, postId])

    return res.send([true])

});

module.exports = router;