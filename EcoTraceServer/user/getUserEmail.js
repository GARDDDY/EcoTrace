const express = require('express');

const connections = require("../server");
const connection2 = connections["users"];

const router = express.Router();

router.get('/getUserEmail', async (req, res) => {
    console.log("fetching");
    const userId = req.query.lgn || '0';
    const pass = req.query.pss || '0';
    console.log("fetching");

    if (!connection2) {
        console.error("not connected to events");
        return;
    }
    console.log("fetching1");

    res.setHeader('Content-Type', 'application/json; charset=utf-8');


    console.log("fetching2")
    const [email] = await connection2.execute(
        `SELECT email FROM user WHERE (username = ? or email = ?) and password = ?`, [userId, userId, pass]
    );

    console.log(email);

    res.send(email.length > 0 && email[0].email || null);
});

module.exports = router;
