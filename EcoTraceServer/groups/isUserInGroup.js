const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();

router.get('/isUserInGroup', async (req, res) => {
    const groupId = req.query.gid || "0";
    const userId = req.query.cid || "0";
    const oauth = req.query.oauth || "0";

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    try {
        const [userInGroup] = await connection1.execute('SELECT * FROM `groups` WHERE userId = ? and groupId = ?', [userId, groupId]);

        res.send([userInGroup.length !== 0]);

    } catch (error) {
        console.error('Error fetching group details:', error);
        res.status(500).json({ error: 'Error fetching group details' });
    }
});

module.exports = router;
