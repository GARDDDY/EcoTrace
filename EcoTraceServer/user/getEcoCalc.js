const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["calculator"]

const router = express.Router();

router.get('/getEcoCalc', async (req, res) => {
    const calcType = req.query.calcType || "";
    const userId = req.query.cid || '0';
    const oauth = req.query.oauth || '0';

    if (!connection2) {
        console.error("not connected to groups")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {

        const [lastGot] = await connection1.execute(`select lastApplied${calcType} from calcexp where userId = ?`, [userId])
        const lastUpdate = lastGot[0][`lastApplied${calcType}`]
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const tomorrow = new Date(today);
        tomorrow.setDate(today.getDate() + 1);

        console.warn(lastGot)
        
        const isUpdating = lastUpdate >= today.getTime() && lastUpdate < tomorrow.getTime();
        const firstTime = lastUpdate == 0;
        const daysSinceLast = Math.floor((new Date() - new Date(lastUpdate)) / (1000 * 60 * 60 * 24));
        console.log(lastUpdate, daysSinceLast & firstTime)

        const [columns] = await connection2.execute(`
            SELECT COLUMN_NAME 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_NAME = 'userstatics' AND COLUMN_NAME LIKE ?`, [`%${calcType}%`]);

        const columnNames = columns.map(col => col.COLUMN_NAME).join(', ');

        if (columnNames.length === 0) {
            return res.send(null);
        }

        const query = `SELECT ${columnNames} FROM userstatics WHERE userId = ?`;
        const [statics] = await connection2.execute(query, [userId]);

        res.json({
            upd: isUpdating,
            first: firstTime,
            days: daysSinceLast & firstTime,
            data: statics[0]
        });

    } catch (error) {
        console.error('Error fetching group details:', error);
        res.status(500).json({ error: 'Error fetching group details' });
    }
});

module.exports = router;
