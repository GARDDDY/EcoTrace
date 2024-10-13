const express = require('express');
//to sql
const { checkOAuth2 } = require('../tech/oauth');
const { getUsernameOnly } = require('../tech/getUsernameOnly');
const { sendMsg } = require('../global/sendMessageToUserDevice');

const router = express.Router();


router.get('/addFriend', async (req, res) => {

    const userId = req.query.uid;
    const requestFrom = req.query.cid || '0';
    const oAuth = req.query.oauth || '0'

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oAuth, requestFrom)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }

    admin.database().ref("users/"+userId+"/friends/"+requestFrom).set(
        {
            friend: false,
            sender: false // is userId a sender
        }
    )
    admin.database().ref("users/"+requestFrom+"/friends/"+userId).set(
        {
            friend: false,
            sender: true
        }
    )

    const senderName = await getUsernameOnly(requestFrom);

    sendMsg(userId, {
        title: "Новый запрос на добавление в друзья",
        body: `${senderName} хочет добавить Вас в друзья!`,
        action: "OPEN_PROFILE",
        data: {
            sender: requestFrom
        }
    })


});

module.exports = router