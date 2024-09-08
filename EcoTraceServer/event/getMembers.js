const express = require('express');

const connections = require("../server")
const connection2 = connections["users"]

const router = express.Router();

router.get('/getEventMembers', async (req, res) => {
    const eventId = req.query.eventId || '0';
    let block = req.query.startAfter || null;
    if (block === "null") block = null;
    let uname = req.query.username || null;
    if (uname === "null") uname = null;

    if (!connection2) {
        console.error("Not connected to users");
        return res.status(500).json({ error: "Not connected to users" });
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {
        // Запрашиваем пользователей по событию
        const [users] = await connection2.execute(
            `SELECT eventRole, userId FROM events WHERE eventId = ? AND (eventId > ? OR ? IS NULL) ORDER BY eventRole LIMIT 100`,
            [eventId, block, block]
        );

        // Получаем все данные о пользователях
        const data = await Promise.all(users.map(async (element) => {
            const [u] = await connection2.execute(
                `SELECT username, experience FROM user WHERE userId = ? AND username LIKE ?`,
                [element.userId, uname ? `${uname}%` : '%'] // Фильтруем по username, начинающемуся с uname
            );

            if (u.length > 0) {
                return {
                    userId: element.userId,
                    username: u[0]?.username || 'Unknown',
                    experience: u[0]?.experience || 0,
                    role: element.eventRole
                };
            }
            return null; // Игнорируем пользователей, не удовлетворяющих условию
        }));

        // Фильтруем null значения и берем только 9 пользователей
        const filteredData = data.filter(item => item !== null).slice(0, 9);

        res.json(filteredData);

    } catch (error) {
        console.error('Error fetching event members:', error);
        res.status(500).json({ error: 'Error fetching event members' });
    }
});

module.exports = router;
