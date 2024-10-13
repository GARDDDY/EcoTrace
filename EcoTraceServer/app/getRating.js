const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]

const router = express.Router();

router.get('/getUserRating', async (req, res) => {
    const userId = req.query.cid || '0';
    const inCountry = req.query.country != "false";

    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    console.log(inCountry)
    console.log(inCountry === true)
    console.log(false === true)
    console.log(true === true)
    var userCountry = null;
    if (inCountry === true) {
        const [c] = await connection1.execute("select country_code from user where userId = ?", [userId])
        console.log(c)
        userCountry = c[0].country_code

        
    }
    console.log(userCountry)

    const [rating] = await connection1.execute("with ranks as (select distinct experience from user order by experience desc limit 30) select userId, username, experience from user where experience in (select experience from ranks) and (? is null or country_code = ?) order by experience desc", [userCountry, userCountry])

    res.json(rating);
});

module.exports = router;
