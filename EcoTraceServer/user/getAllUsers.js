const express = require('express');

const connections = require("../server")
const connection2 = connections["users"]

const router = express.Router();

router.get('/getAllUsers', async (req, res) => {
    const filters = req.query.filters || "";
    const userId = req.query.cid || '0';
    var startAfter = req.query.nei || null;
    var filterName = req.query.name || null;
    if (filterName === "null") filterName = null;
    if (startAfter === "null") startAfter = null;

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const [users] = await connection2.execute(`select userId, username, filters from user 
        where filters like ? and userId != ? and (? is null or username like ?) and (? is null or userId > ?) 
        limit 3`, [`%${filters}%`, userId, filterName, `%${filterName}%`, startAfter, startAfter])

    console.log(userId, users)

    const [total] = await connection2.execute(`select count(*) as total from user 
        where filters like ? and userId != ? and (? is null or username like ?) and (? is null or userId > ?)`, 
        [`%${filters}%`, userId, filterName, `%${filterName}%`, startAfter, startAfter])

    const hasMore = users.length === 3 && total[0].total > 3;

    res.json([!hasMore, users]);

    
});

module.exports = router;
