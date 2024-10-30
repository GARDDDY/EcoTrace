const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection = connections["users"]

const router = express.Router();


router.get('/getUserPrivate', async (req, res) => {
    const userId = req.query.cid || '0';
    const oAuth = req.query.oauth || '0';

    console.log(userId)

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oAuth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }

    try {
        const [private] = await connection.execute(
            `SELECT email, fullname FROM user where userId = "${userId}"` // gender???
        );

        const privateArray = private[0];
        privateArray.email = `${privateArray.email[0]}***${privateArray.email.match("@[\\w\\d\\.]+")}`

        res.json(privateArray);

    } catch (error) {
        console.error(error);
        res.status(500).json({ error: "Internal server error" });
    }

});

module.exports = router;
