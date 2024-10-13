const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();


const roleToCount = {
    0: 1,
    1: 5,
    2: 10,
    3: 30
}

router.get('/getGroupMembers', async (req, res) => {
    const groupId = req.query.gid || "0";
    var lastGot = req.query.lastGot || null;
    if (lastGot == "null") lastGot = null
    const role = req.query.role || 3;

    const userId = req.query.cid || "0";
    const oauth = req.query.oauth || "0";

    if (!connection2) {
        console.error("not connected to groups")
        return;
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {
        const [users] = await connection1.execute(`SELECT userId, role FROM \`groups\` WHERE groupId = ? and role = ? limit ${roleToCount[role]}`, [groupId, role]);

        if (users.length > 0) {

            const usersIds = users.map(user => user.userId);
            const placeholders = usersIds.map(() => '?').join(', ');
            const [usernames] = await connection1.execute(`SELECT userId, username FROM \`user\` WHERE userId IN (${placeholders})`, usersIds);
        
            const creatorsMap = Object.fromEntries(usernames.map(user => [user.userId, user.username]));

            for (const user of users){
                user.username = creatorsMap[user.userId] || "NONE"
            }
        }


        const [morePosts] = await connection1.execute(
            'SELECT COUNT(*) as total FROM `groups` WHERE groupId = ? AND role = ?',
            [groupId, role]
        );
        const hasMore = morePosts[0].total > roleToCount[role] && posts.length === roleToCount[role];

        res.send([hasMore, users]);

    } catch (error) {
        console.error('Error fetching group details:', error);
        res.status(500).json({ error: 'Error fetching group details' });
    }
});

module.exports = router;
