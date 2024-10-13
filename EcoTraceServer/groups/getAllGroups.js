const express = require('express');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();

router.get('/getAllGroups', async (req, res) => {
    const filters = req.query.filters || "";
    var startAfter = req.query.nei || null;
    if (startAfter == "null") startAfter = null;

    if (!connection2) {
        console.error("not connected to groups")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {
        let query = 'SELECT * FROM `group` WHERE filters LIKE ?';
        let params = [`%${filters}%`];

        if (startAfter !== null) {
            query += ' AND groupId > ?';
            params.push(startAfter);
        }
        query += ' LIMIT 3'

        const [groups] = await connection2.execute(query, params);

        let countQuery = 'SELECT COUNT(*) as total FROM `group` WHERE filters LIKE ?';
        let countParams = [`%${filters}%`];

        if (startAfter !== null) {
            countQuery += ' AND groupId > ?';
            countParams.push(startAfter);
        }

        const [countResult] = await connection2.execute(countQuery, countParams);
        const totalRecords = countResult[0].total;

        const hasMore = groups.length === 3 && totalRecords > 3;


        const usersIds = [...new Set(groups.map(group => group.groupCreatorId))];
        const userIds = usersIds.map(id => `'${id}'`).join(', ');
        
        let usernames = {};
        
        if (userIds.length > 0) {
            const userQuery = `SELECT userId, username FROM user WHERE userId IN (${userIds})`;
            const [users] = await connection1.execute(userQuery);
            usernames = users.reduce((acc, user) => {
                acc[user.userId] = user.username;
                return acc;
            }, {});
        }

        const groupsIds = [...new Set(groups.map(group => group.groupId))];
        const groupIds = groupsIds.map(id => `'${id}'`).join(', ');

        let groupMembers = {}
        if (groupIds.length > 0) {
            const [members] = await connection1.execute(`SELECT groupId, COUNT (*) AS total FROM \`groups\` WHERE groupId in (${groupIds}) group by groupId`)
            groupMembers = members.reduce((acc, user) => {
                acc[user.groupId] = user.total;
                return acc;
            }, {});
        }
    
        const formattedGroups = groups.map(group => ({
            ...group,
            groupCreatorName: usernames[group.groupCreatorId] || null,
            groupCountMembers: groupMembers[group.groupId] || 0
        }));

        res.json([!hasMore, formattedGroups]);

    } catch (error) {
        console.error('Error fetching group details:', error);
        res.status(500).json({ error: 'Error fetching group details' });
    }
});

module.exports = router;
