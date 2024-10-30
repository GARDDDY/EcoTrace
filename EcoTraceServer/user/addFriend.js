const express = require('express');

const { checkOAuth2 } = require('../tech/oauth');
const { sendMsg } = require('../global/sendMessageToUserDevice');
const connections = require("../server")
const connection1 = connections["users"]

const router = express.Router();

router.get('/addFriend', async (req, res) => {

    const userId = req.query.uid;
    const requestFrom = req.query.cid || '0';
    const oAuth = req.query.oauth || '0'

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oAuth, requestFrom)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }

    const [any] = await connection1.execute('select * from friends where userId = ? and senderId = ? or userId = ? and senderId = ?', [userId, requestFrom, requestFrom, userId])

    if (any.length == 0) {
        await connection1.execute("insert into friends (senderId, userId, isFriend) values (?,?,?)", [requestFrom, userId, 0])
    } else {
        if (requestFrom == any[0].userId) {
            await connection1.execute("update friends set isFriend = 1 where userId = ? and senderId = ? or userId = ? and senderId = ?", [userId, requestFrom, requestFrom, userId])
        }
    }

    
    res.send([true])

});

module.exports = router