const express = require('express');

const { checkOAuth2 } = require('../tech/oauth');
const connections = require("../server")
const connection1 = connections["users"]

const router = express.Router();

router.get('/removeFriend', async (req, res) => {

    const userId = req.query.uid;
    const requestFrom = req.query.cid || '0';
    const oAuth = req.query.oauth || '0'

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oAuth, requestFrom)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }

    await connection1.execute("delete from friends where userId = ? and senderId = ? or userId = ? and senderId = ?", [userId, requestFrom, requestFrom, userId])
    res.send([true])

});

module.exports = router