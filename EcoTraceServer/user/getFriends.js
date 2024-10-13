const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]

const router = express.Router();

router.get('/getUserFriends', async (req, res) => {
    const userId = req.query.uid || '0';
    const cUserId = req.query.cid || '0';
    const oauth = req.query.oauth || '0';
    const block = req.query.block || null;

    try {
        const [rule] = await connection1.execute(`SELECT canSeeFriends FROM rules WHERE userId = ?`, [userId]);

        if (!await checkOAuth2(oauth, cUserId)) {
            return res.status(403).json({ error: 'Forbidden' });
        }

        const canSeeFriends = rule[0]?.canSeeFriends;

        if (userId === cUserId) {
            const [friends] = await connection1.execute(`
                SELECT f.*, 
                CASE 
                    WHEN f.userId = ? THEN u1.username 
                    ELSE u2.username 
                END AS username
                FROM friends f
                LEFT JOIN user u1 ON u1.userId = f.senderId 
                LEFT JOIN user u2 ON u2.userId = f.userId 
                WHERE (f.userId = ? OR f.senderId = ?)
                LIMIT 6
            `, [userId, userId, userId]);

            return res.json(friends)

        } else if (canSeeFriends === 2) {
            return res.json([])
        }
        else if (canSeeFriends === 1) {

            const [friend] = await connection1.execute(`
                SELECT isFriend 
                FROM friends 
                WHERE (userId = ? AND senderId = ?) OR (userId = ? AND senderId = ?)
            `, [cUserId, userId, userId, cUserId]);

            if (friend.length === 0 || friend[0].isFriend === 0) {
                return res.json([]);
            }
        }
        
        const [friends] = await connection1.execute(`
            SELECT f.*, 
            CASE 
                WHEN f.userId = ? THEN u1.username 
                ELSE u2.username 
            END AS username
            FROM friends f
            LEFT JOIN users u1 ON u1.userId = f.senderId 
            LEFT JOIN users u2 ON u2.userId = f.userId 
            WHERE (f.userId = ? OR f.senderId = ?) AND f.isFriend = 1 
            LIMIT 6
        `, [userId, userId, userId]);

        return res.json(friends);
    } catch (error) {
        console.error("Error fetching user friends: ", error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

module.exports = router;
