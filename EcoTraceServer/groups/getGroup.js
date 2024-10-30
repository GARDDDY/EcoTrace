const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();

router.get('/getGroup', async (req, res) => {
    const groupId = req.query.gid || "";
    // todo for oauth

    if (!connection2) {
        console.error("not connected to groups")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {
        const [group] = await connection2.execute('SELECT * FROM `group` WHERE groupId = ?', [groupId]);

        const [members] = await connection1.execute('SELECT COUNT (*) AS total FROM `groups` WHERE groupId = ?', [groupId])
        const [creator] = await connection1.execute('SELECT username FROM `user` WHERE userId = ?', [group[0].groupCreatorId])

        group[0].groupCountMembers = members[0].total
        group[0].groupCreatorName = creator[0].username

        res.json(group[0]);

    } catch (error) {
        console.error('Error fetching group details:', error);
        res.status(500).json({ error: 'Error fetching group details' });
    }
});

module.exports = router;
