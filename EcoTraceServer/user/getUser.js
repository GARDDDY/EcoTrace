const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection = connections["users"]

const router = express.Router();

function merge(userData) {
    userData.country = {
        code: userData.country_code,
        name: userData.country_name
    }
    delete userData.country_code;
    delete userData.country_name;

    return userData
}


router.get('/getUser', async (req, res) => {

    if (!connection) {
        console.error("not connected to users")
        return
    }

    const userId = req.query.uid || '0';
    const requestFrom = req.query.cid || '0';
    const oAuth = req.query.oauth || '0';

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    // if (!await checkOAuth2(oAuth, requestFrom)) {
    //     return res.status(403).json({ error: "You are not signed in! Not allowed" });
    // }

    try {
        const [rows] = await connection.execute(
            `SELECT * FROM user where userId = "${userId}"`
        );
        
        if (rows.length === 0) {
            return res.status(404).json({ error: "User not found" });
        }

        const userData = merge(rows[0]);

        const [rulesRows] = await connection.execute(
            `SELECT * FROM rules WHERE userId = "${userId}"`
        );
        const rules = rulesRows[0] || {};

        const [friendRows] = await connection.execute(
            `SELECT * FROM friends WHERE userId = "${userId}" AND senderId = "${requestFrom}"`
        );
        const isFriend = friendRows.length > 0;

        const isOwner = userId === requestFrom;

        delete userData.email;
        delete userData.password;

        function isNotAvailableToGet(collectionType) {
            if (isOwner) return false;
            if (rules?.[collectionType] === 0) return false;
            if (rules?.[collectionType] === 1) return !isFriend;
            return true;
        }

        if (isNotAvailableToGet('canSeeCountry')) {
            delete userData.country;
        }
        if (isNotAvailableToGet('canSeeFullname')){
            delete userData.fullname;
        }

        res.json(userData);

    } catch (error) {
        console.error(error);
        res.status(500).json({ error: "Internal server error" });
    }








    // const userId = req.query.uid || '0';
    // const requestFrom = req.query.cid || '0';
    // const oAuth = req.query.oauth || '0';

    // res.setHeader('Content-Type', 'application/json; charset=utf-8');

    // if (!await checkOAuth2(oAuth, requestFrom)) {
    //     return res.status(403).json({ error: "You are not signed in! Not allowed" });
    // }

    // const ref = admin.database().ref('users/' + userId);
    // const snap = await ref.once('value');
    // const userData = snap.val();

    // const rules = userData.rules || {}; // !!!

    // const isFriend = userData?.friends?.[requestFrom];
    // const isOwner = userId === requestFrom;

    // delete userData.private;
    // delete userData.rules;
    // delete userData.friends;
    // delete userData.events;
    // delete userData.groups;

    // function isNotAvailableToGet(collectionType) {
    //     if (isOwner) return false;
    //     if (rules?.[collectionType] === 0) return false;
    //     if (rules?.[collectionType] === 1) return !isFriend;
    //     return true;
    // }

    // if (isNotAvailableToGet('countrySeen')) {
    //     delete userData.country;
    // }
    // if (isNotAvailableToGet('nameSeen')){
    //     delete userData.fullname;
    // }

    // res.json(userData);
});

module.exports = router;
