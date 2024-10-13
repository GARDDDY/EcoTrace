const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["groups"]

const router = express.Router();

router.get('/isGroupNameAvailable', async (req, res) => {
    const name = req.query.name || '';

    const forbiddenChars = /[^a-zA-Z0-9а-яА-ЯёЁ\s-\.!#]/;
    const invalidChar = name.split('').find(char => forbiddenChars.test(char));
    if (invalidChar) {
        return res.send([`Запрещенный символ: ${invalidChar}`]);
    }
    if (name.length < 5) {
        return res.send(["Название от 5 символов"]);
    }
    
    const [others] = await connection1.execute("SELECT groupId FROM `group` WHERE groupName = ?", [name]);
    
    if (others.length === 0) {
        res.send([null]);
    } else {
        res.send(["Такое имя уже занято!"]);
    }
    
});

module.exports = router;
