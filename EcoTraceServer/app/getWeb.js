const express = require('express');

const connections = require("../server")
const connection2 = connections["web"]

const router = express.Router();


router.get('/getWeb', async (req, res) => {
    const [posts] = await connection2.execute(
        `select * from posts`
    )

    res.send(posts);
});

module.exports = router;