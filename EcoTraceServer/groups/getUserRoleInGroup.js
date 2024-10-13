const express = require('express');

const connections = require("../server")
const connection2 = connections["users"]

const router = express.Router();


router.get('/getUserRoleInGroup', async (req, res) => {
    const userId = req.query.cid || '0';
    const groupId = req.query.gid || '0';
    const oauth = req.query.oauth || '0';

    const [role] = await connection2.execute(
        'select role from `groups` where userId = ? and groupId = ?', [userId, groupId]
    )

    res.send([role.length > 0 ? role[0].role : 4]);
});

module.exports = router;