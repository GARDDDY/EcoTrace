const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]

const router = express.Router();

router.get('/getUserFriends', async (req, res) => {

    // todo! redo all
    const userId = req.query.uid || '0';
    const cUserId = req.query.cid || '0';
    const oauth = req.query.oauth || '0';
    const block = req.query.block || null;

    try {
        const [rule] = await connection1.execute(`SELECT canSeeFriends FROM rules WHERE userId = ?`, [userId]);

        if (!await checkOAuth2(oauth, cUserId)) {
            return res.status(403).send([null]); // Forbidden
        }

        const canSeeFriends = rule[0]?.canSeeFriends;

        if (userId === cUserId) {
            // const [friends] = await connection1.execute(`
            //     SELECT f.*, 
            //     CASE 
            //         WHEN f.userId = ? THEN u1.username 
            //         ELSE u2.username 
            //     END AS username
            //     FROM friends f
            //     LEFT JOIN user u1 ON u1.userId = f.senderId 
            //     LEFT JOIN user u2 ON u2.userId = f.userId 
            //     WHERE (f.userId = ? OR f.senderId = ?)
            //     ${block !== null ? 'AND f.userId > ?' : ''}
            //     LIMIT 6
            // `, block !== null ? [userId, userId, userId, block] : [userId, userId, userId]);
            const [friends] = await connection1.execute(`select user.username, friends.*
                from friends
                inner join user ON 
                (friends.senderId != ? and friends.senderId = user.userId)
                or
                (friends.userId != ? and friends.userId = user.userId)
                where (? is null or friends.senderId > ? or friends.userId > ?)
                limit 6`, [userId, userId, block, block, block])

            return res.json(friends)

        } else if (canSeeFriends === 2) { // no-one can see
            return res.json([])
        }
        else if (canSeeFriends === 1) { // friends only

            const [friend] = await connection1.execute(`
                SELECT isFriend 
                FROM friends 
                WHERE (userId = ? AND senderId = ?) OR (userId = ? AND senderId = ?)
            `, [cUserId, userId, userId, cUserId]);

            console.log("friend", friend)

            if (friend.length === 0 || friend[0].isFriend === 0) {
                return res.json([]);
            }
        }
        
        const [friends] = await connection1.execute(`select user.username, friends.*
                from friends
                inner join user ON 
                (friends.senderId != ? and friends.senderId = user.userId)
                or
                (friends.userId != ? and friends.userId = user.userId)
                where isFriend = 1 and (? is null or friends.senderId > ? or friends.userId > ?)
                limit 6`, [userId, userId, block, block, block]);

        const friendsAnon = friends.map(friend => {
            if (friend.senderId === userId) {
              return { senderId: friend.userId, userId: friend.userId, username: friend.username, isFriend: friend.isFriend };
            } else if (friend.userId === userId) {
              return { senderId: friend.senderId, userId: friend.senderId, username: friend.username, isFriend: friend.isFriend };
            }
          }).filter(Boolean);

          console.log(friends)
          console.log(friendsAnon)

        return res.json(friendsAnon);
    } catch (error) {
        console.error("Error fetching user friends: ", error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

module.exports = router;
