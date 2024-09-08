const express = require('express');
const admin = require('firebase-admin');
const { checkOAuth2 } = require('../tech/oauth');
const { getRules } = require('../tech/getUserRules');
const { areUsersFriends } = require('../tech/areUsersFriends');

const router = express.Router();

router.get('/getUserGroups', async (req, res) => {
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
    async function getGroups(block, userId, requestFrom, repeated = false) {
        let isOwner = requestFrom === userId && userId !== '0';
        let ref = admin.database().ref(`users/${userId}/groups`).orderByKey();

        if (block !== null) {
            ref = ref.startAfter(block);
        }

        let snapshot = await ref.limitToFirst(2).once('value');
        let data = snapshot.val() || {};

        let lastGid = null;
        let groupsRule = await getRules(userId, 3);
        let isFriend = await areUsersFriends(userId, requestFrom);

        let filteredData = {};

        for (let gid in data) {
            let gData = data[gid];
            let shouldKeep = isOwner || 
                (groupsRule === 0 && gData) || 
                (groupsRule === 1 && isFriend && gData);

            if (shouldKeep) {
                filteredData[gid] = gData;
            }

            lastGid = gid;
        }

        if (Object.keys(filteredData).length >= 2 || repeated) {
            return filteredData;
        } else {
            block = lastGid;
            let moreData = await getGroups(block, userId, requestFrom, true);
            return { ...filteredData, ...moreData };
        }
    }

    try {
        let dataGlobal = await getGroups(block, userId, requestFrom);
        res.json(dataGlobal);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
