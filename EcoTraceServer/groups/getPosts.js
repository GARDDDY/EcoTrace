const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();

router.get('/getPosts', async (req, res) => {
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

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {
        const [userInGroup] = await connection1.execute('SELECT * FROM `groups` WHERE userId = ? and groupId = ?', [userId, groupId]);

        if (userInGroup.length === 0) {
            return res.send([false, null]);
        }

        const [posts] = await connection2.execute(
            'SELECT * FROM `posts` WHERE groupId = ? AND (? IS NULL OR postId < ?) AND postTime <= ? ORDER BY postTime desc limit 5',
            [groupId, lastGot, lastGot, timestamp]
        );
        const [morePosts] = await connection2.execute(
            'SELECT COUNT(*) as total FROM `posts` WHERE groupId = ? AND (? IS NULL OR postId < ?) AND postTime <= ?',
            [groupId, lastGot, lastGot, timestamp]
        );
        const hasMore = morePosts[0].total > 5 && posts.length === 5;


        if (posts.length > 0) {
            const usersIds = posts.map(post => post.postCreatorId);
            const postIds = posts.map(post => post.postId);
            
        
            const users = usersIds.map(user => `"${user}"`).join(', ');
            const [creators] = await connection1.execute(`SELECT userId, username FROM \`user\` 
                WHERE userId IN (${users})`);
        
            const creatorsMap = Object.fromEntries(creators.map(creator => [creator.userId, creator.username]));
            console.log(creatorsMap);
        
            
            const postsids = postIds.map(post => `${post}`).join(', ');
            const [commentsCounts] = await connection2.execute(`SELECT postId, COUNT(*) AS total FROM \`comments\` 
                WHERE groupId = ? AND postId IN (${postsids}) 
                GROUP BY postId`, 
                [groupId]);

            console.warn("comms", commentsCounts)
            console.warn("comms", postsids)
            const commentsMap = Object.fromEntries(commentsCounts.map(comment => [comment.postId, comment.total]));
            console.log(commentsMap)

            const [roles] = await connection1.execute(`SELECT userId, \`role\` FROM \`groups\` 
                WHERE userId IN (${users}) AND groupId = ?`, 
                [groupId]);

            const rolesMap = Object.fromEntries(roles.map(role => [role.userId, role.role]));
            console.log("roles", rolesMap)
        
            for (const post of posts) {
                post.postCreatorName = creatorsMap[post.postCreatorId] || 'NONE';
                post.postCommentsCount = commentsMap[post.postId] || 0;
                post.postCreatorRole = rolesMap[post.postCreatorId] !== undefined ? rolesMap[post.postCreatorId] : 4;
            }
        }
        

        res.send([hasMore, posts]);

    } catch (error) {
        console.error('Error fetching group details:', error);
        res.status(500).json({ error: 'Error fetching group details' });
    }
});

module.exports = router;
