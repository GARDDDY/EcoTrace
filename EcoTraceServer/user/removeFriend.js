const express = require('express');
const admin = require('firebase-admin');
const { checkOAuth2 } = require('../tech/oauth');

const router = express.Router();

router.get('/removeFriend', async (req, res) => {

    const userId = req.query.uid;
    const requestFrom = req.query.cid || '0';
    const oAuth = req.query.oauth || '0'

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oAuth, requestFrom)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }

    admin.database().ref("users/"+userId+"/friends/"+requestFrom).remove();
    admin.database().ref("users/"+requestFrom+"/friends/"+userId).remove();

});

module.exports = router