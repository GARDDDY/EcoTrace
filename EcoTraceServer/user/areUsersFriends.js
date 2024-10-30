const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection = connections["users"]

const router = express.Router();



router.get('/areUsersFriends', async (req, res) => {

    if (!connection) {
        console.error("not connected to users")
        return
    }

    const requestFrom = req.query.cid || '0';
    const oAuth = req.query.oauth || '0';
    const userId = req.query.uid || '0';

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oAuth, requestFrom)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }

    const [fr] = await connection.execute("select * from friends where userId = ? and senderId = ? or userId = ? and senderId = ?", [requestFrom, userId, userId, requestFrom])

    if (fr.length === 0){
        return res.send([0]) // not at all
    } 
    if (fr[0].isFriend === 1) {
        return res.send([3]) // fully
    } 
    
    if (fr[0].senderId === requestFrom) {
        return res.send([1]) // requestFrom sent request
    } 
    if (fr[0].userId === requestFrom) {
        return res.send([2]) // requestFrom got request
    }
    
    res.send([false])
});

module.exports = router;
