const express = require('express');
const admin = require('firebase-admin');
const { checkOAuth2 } = require('../tech/oauth');
const { getRules } = require('../tech/getUserRules');
const { areUsersFriends } = require('../tech/areUsersFriends');
const { getUsernameOnly } = require('../tech/getUsernameOnly');

const router = express.Router();

router.get('/getUserFriends', async (req, res) => {
    let userId = req.query.uid || '0';
    let block = req.query.block || null;
    if (block === 'null') {
        block = null;
    }
    let oAuth = req.query.oauth || '0';
    let requestFrom = req.query.cid || '0';

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oAuth, requestFrom)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }

    async function get(block, userId, requestFrom) {
        let isOwner = requestFrom === userId && userId !== '0';
        let ref = admin.database().ref(`users/${userId}/friends`).orderByKey();

        if (block !== null) {
            ref = ref.startAfter(block);
        }

        let snapshot = await ref.limitToFirst(2).once('value');
        let data = snapshot.val() || {};

        let lastUid = null;
        let friendsRule = await getRules(userId, 2);
        let isFriend = await areUsersFriends(userId, requestFrom);
        let filteredData = {};

        for (let uid in data) {
            let gData = data[uid];
            let shouldKeep = isOwner ||
                (friendsRule === 0 && gData.friend) ||
                (friendsRule === 1 && isFriend && gData.friend);

            if (shouldKeep) {
                filteredData[uid] = gData;
                filteredData[uid].userId = uid;
                filteredData[uid].username = await getUsernameOnly(uid);
            }

            lastUid = uid;
        }

        if (Object.keys(filteredData).length >= 2) {
            return filteredData;
        } else {
            block = lastUid;
            let moreData = await get(block, userId, requestFrom);
            return { ...filteredData, ...moreData };
        }
    }

    try {
        let dataGlobal = await get(block, userId, requestFrom);
        res.json(dataGlobal);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
