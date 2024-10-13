const express = require('express');
const { v4: uuidv4 } = require('uuid');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.post('/register', async (req, res) => {
    // calculator.userstatics
});

module.exports = router;