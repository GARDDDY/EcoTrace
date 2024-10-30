const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection = connections["users"]

const router = express.Router();


router.get('/getUserRules', async (req, res) => {
    const userId = req.query.cid || '0';
    const oAuth = req.query.oauth || '0';

    console.log(userId)

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (!await checkOAuth2(oAuth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }

    try {
        const [rules] = await connection.execute(
            `SELECT * FROM rules where userId = "${userId}"`
        );

        console.log(rules)

        const rulesArray = rules[0]
        delete rulesArray["userId"]

        res.json(rulesArray);

    } catch (error) {
        console.error(error);
        res.status(500).json({ error: "Internal server error" });
    }

});

module.exports = router;
