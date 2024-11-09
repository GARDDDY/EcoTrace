const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server");
const connection1 = connections["users"]

const router = express.Router();


router.get('/eduStatus', async (req, res) => {
    const userId = req.query.cid || '0';
    const oAuth = req.query.oauth || '0';
    const e = req.query.edu || 0

    if (!connection1) {
        console.error("not connected to events or users")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oAuth, userId)) {
        return res.send([false]);
    }

    try {
        const [result] = await connection1.execute(`select edu${e} from education where userId = ?`, [userId])
        console.log(result)

        return res.send([result[0][`edu${e}`] === 1])
    } catch (e) {
        return res.send([false])
    }
});

module.exports = router;
