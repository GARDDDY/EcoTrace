const express = require('express');
const admin = require('firebase-admin');
const { getUsernameOnly } = require('../tech/getUsernameOnly');

const router = express.Router();

router.get('/getUserRating', async (req, res) => {
    let userId = req.query.uid || '0';
    let block = req.query.block || null;
    if (block === 'null') {
        block = null;
    }
    let oAuth = req.query.oauth || '0';
    let requestFrom = req.query.cid || '0';

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const usersRef = admin.database().ref("users").orderByChild("experience").limitToLast(30);
    const snapshot = await usersRef.once('value');
    const values = snapshot.val() || {};

    const usersObject = Object.entries(values).reduce((acc, [uid, uData]) => {
        acc[uid] = {
            experience: uData.experience,
            username: uData.username
        };
        return acc;
    }, {});

    res.json(usersObject);

    
});

module.exports = router;
