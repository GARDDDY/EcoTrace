const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { checkOAuth2 } = require('../tech/oauth');

const t = require("../tech/tAuth")

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["calculator"]
const connection3 = connections["auth"]

const router = express.Router();

const multer = require('multer');
const path = require('path');
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, './EcoTraceServer/groups/uploads');
    },
    filename: function (req, file, cb) {
        cb(null, Date.now() + path.extname(file.originalname));
    }
});

const upload = multer({ storage: storage });

function genId() {
    const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
    let id = '';
    var num = Date.now()
    while (num > 0) {
        id = chars[num % 62] + id;
        num = Math.floor(num / 62);
    }
    return id;
}


router.post('/create', upload.single('image'), async (req, res) => {
    const userData = JSON.parse(req.body.jsonData);

    console.log("got data", userData)

    const user = genId();

    const data = userData.user
    const secrets = userData.secret


    let userCountry;
    const ip = req.ip;

    console.log("ip", ip)

    const response = await fetch(`https://ipinfo.io/${ip}/json`);
    const countryData = await response.json();

    if (countryData.country) {
        userCountry = countryData.country;
        console.log(`Код страны: ${userCountry}`);
    } else {
        userCountry = "RU";
        console.error('Не удалось получить код страны');
    }

    await connection1.execute(`insert into user (userId, about_me, country_code, experience, filters, fullname, gender, email, password, username) values(?,?,?,?,?,?,?,?,?,?)`,
        [user, data.about_me, userCountry, 0, data.filters, data.fullname || null, data.gender, secrets[0], secrets[1], data.username]
    )
    await connection1.execute(`insert into rules (userId, canSeeCountry, canSeeFriends, canSeeFullname, canSeeGroups) values(?,?,?,?,?)`, [user, 0,0,0,0])
    await connection1.execute(`insert into education (userId) values(?)`, [user])
    await connection1.execute(`insert into calcexp (userId, lastApplied0, lastApplied1, lastApplied2, lastApplied3, lastApplied4) values(?,?,?,?,?,?)`, [user, 0,0,0,0,0])

    await connection2.execute(`insert into userstatics (userId) values(?)`, [user])

    const token = await t.gut(user, secrets[1])

    return res.send([user, token])



});

module.exports = router;