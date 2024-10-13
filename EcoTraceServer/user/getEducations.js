const express = require('express');

const connections = require("../server")
const connection2 = connections["users"]

const router = express.Router();

router.get('/getEducations', async (req, res) => {
    const userId = req.query.cid || '0';
    const oAuth = req.query.oauth || '0';
    const sinceTime = req.query.since || 0;

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const [rows] = await connection2.execute(
        `SELECT * FROM education WHERE userId = ?`, [userId]
    );
    
    const eduIndexes = [];
    
    if (rows.length > 0) {
        const userEdu = rows[0];
        Object.keys(userEdu).forEach((key, index) => {
            if (key.startsWith('edu') && userEdu[key] === 1) {
                eduIndexes.push(index);
            }
        });
    }

    res.json(eduIndexes);
});

module.exports = router;
