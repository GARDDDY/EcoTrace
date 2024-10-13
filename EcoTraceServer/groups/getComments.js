const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();

router.get('/getComments', async (req, res) => {
    const groupId = req.query.gid || "0";
    const postId = req.query.pid || '0';
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
            return res.send([]);
        }

        const [comments] = await connection2.execute(
            'SELECT * FROM `comments` WHERE groupId = ? and postId = ? AND (? IS NULL OR postId > ?) limit 4',
            [groupId, postId, lastGot, lastGot]
        );

        if (comments.length > 0) {
            const usersIds = comments.map(post => post.commentCreatorId);
            
        
            const users = usersIds.map(user => `"${user}"`).join(', ');
            const [creators] = await connection1.execute(`SELECT userId, username FROM \`user\` 
                WHERE userId IN (${users})`);
        
            const creatorsMap = Object.fromEntries(creators.map(creator => [creator.userId, creator.username]));
            console.log(creatorsMap);

            const [roles] = await connection1.execute(`SELECT userId, \`role\` FROM \`groups\` 
                WHERE userId IN (${users}) AND groupId = ?`, 
                [groupId]);

            const rolesMap = Object.fromEntries(roles.map(role => [role.userId, role.role]));
            console.log("roles", rolesMap)
        
            for (const post of comments) {
                post.commentCreatorName = creatorsMap[post.commentCreatorId] || 'NONE';
                post.commentCreatorRole = rolesMap[post.commentCreatorId] !== undefined ? rolesMap[post.commentCreatorId] : 4;
            }
        }
        

        res.send(comments);

    } catch (error) {
        console.error('Error fetching group details:', error);
        res.status(500).json({ error: 'Error fetching group details' });
    }
});

module.exports = router;
