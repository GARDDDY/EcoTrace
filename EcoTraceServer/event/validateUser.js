const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.get('/validateUser', async (req, res) => { // check todo
    const userIdToValidate = req.query.uid;
    const userId = req.query.cid || '0';
    const oauth = req.query.oauth || '0';
    const eventId = req.query.eid || '0';


    if (!connection1) {
        console.error("not connected to users")
        return
    }

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const [moder] = await connection1.execute(
        `SELECT eventRole FROM events WHERE eventId = "${eventId}" AND userId = "${userId}"`
    );
    const isModer = [moder[0].eventRole <= 1];

    if (!isModer) {
        res.send(false);
        return;
    }

    await connection1.execute(
        `UPDATE events SET validated = true WHERE userId = ? and eventId = ?`,
        [userIdToValidate, eventId]
    );

    const [eData] = await connection2.execute(
        `select eventCreatorId from event WHERE eventId = ?`,
        [eventId]
    );
    const [members] = await connection1.execute('select count(*) as total from events where eventId = ?', [eventId])
    const [userRole] = await connection1.execute('select eventRole, validated from events where userId = ? and eventId = ?', [userIdToValidate, eventId])

    if (userRole[0].validated === 1) {
        return res.send([false])
    }

    const [creatorExp] = await connection1.execute(
        `select experience from user WHERE userId = ?`,
        [eData[0].eventCreatorId]
    );
    
    const [currentExp] = await connection1.execute(
        `select experience from user WHERE userId = ?`,
        [userIdToValidate]
    );

    const addExp = Math.floor((creatorExp[0]*0.01 + members[0].total * (currentExp + 1)*0.002)*(2 - userRole[0].userRole))
    console.log(addExp);

    res.send([true]);
});

module.exports = router;
