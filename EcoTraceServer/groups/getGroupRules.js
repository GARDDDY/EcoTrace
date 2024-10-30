const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();
const { checkOAuth2 } = require('../tech/oauth');

router.get('/getGroupRules', async (req, res) => {
    const groupId = req.query.gid || "0";
    
    const [rules] = await connection2.execute("select groupRules, groupRulesImage from `group` where groupId = ?", [groupId])

    res.send([rules[0].groupRules, rules[0].groupRulesImage])

});

module.exports = router;
