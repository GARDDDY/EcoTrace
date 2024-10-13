const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();

router.get('/getNewPosts', async (req, res) => {
    const groupId = req.query.gid || "0";
    const userId = req.query.cid || "0";
    const oauth = req.query.oauth || "0";
    var lastGot = req.query.lastGot || null;
    if (lastGot === "null") lastGot = null;

    const timestamp = new Date().getTime() / 1000;

    if (!connection2) {
        console.error("not connected to groups")
        return;
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {
        const [userInGroup] = await connection1.execute('SELECT * FROM `groups` WHERE userId = ? and groupId = ?', [userId, groupId]);

        if (userInGroup.length === 0) {
            return res.send([null]);
        }

        const [posts] = await connection2.execute(
            'SELECT * FROM `posts` WHERE groupId = ? AND (? IS NULL OR postId > ?) ORDER BY postId',
            [groupId, lastGot, lastGot]
        );

        res.send(posts.length == 0 ? [null] : posts);

    } catch (error) {
        console.error('Error fetching group details:', error);
        res.status(500).json({ error: 'Error fetching group details' });
    }
});

module.exports = router;
