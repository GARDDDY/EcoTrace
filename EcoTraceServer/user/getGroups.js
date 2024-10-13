const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["groups"]

const router = express.Router();

router.get('/getUserGroups', async (req, res) => {
    const userId = req.query.uid || '0';
    const cUserId = req.query.cid || '0';
    const oauth = req.query.oauth || '0';
    const block = req.query.block || null;
    // const sort = req.query.sort || '-1';

    try {
        const [rule] = await connection1.execute(`SELECT canSeeGroups FROM rules WHERE userId = ?`, [userId]);

        if (!await checkOAuth2(oauth, cUserId)) {
            return res.status(403).json({ error: 'Forbidden' });
        }

        const canSeeGroups = rule[0]?.canSeeGroups;

        if (canSeeGroups === 2 && userId !== cUserId) {
            return res.json([])
        }
        else if (canSeeGroups === 1) {

            const [friend] = await connection1.execute(`
                SELECT isFriend 
                FROM friends 
                WHERE (userId = ? AND senderId = ?) OR (userId = ? AND senderId = ?)
            `, [cUserId, userId, userId, cUserId]);

            if (friend.length === 0 || friend[0].isFriend === 0) {
                return res.json([]);
            }
        }

        const [groups] = await connection1.execute(`
            SELECT groupId, role FROM \`groups\` where userId = ? and (? is null or groupId > ?) LIMIT 3`,
        [userId, block, block]);

        const groupsIds = groups.map(group => group.groupId);
        const groupsId = groupsIds.map(id => `'${id}'`).join(', ');

        let groupsData = [];
        if (groupsId.length > 0) {
            const [results] = await connection2.execute(`
                SELECT groupId, groupName, groupAbout
                FROM \`group\` 
                WHERE groupId IN (${groupsId})
            `);
            
            groupsData = results;
        }

        const combinedResults = groups.map(group => {
            const relatedEvent = groupsData.find(data => data.groupId === group.groupId);
            return {
                groupRole: group.role,
                groupInfo: {
                    groupId: group.groupId,
                    groupName: relatedEvent ? relatedEvent.groupName : null,
                    groupAbout: relatedEvent ? relatedEvent.groupAbout : null,
                }
            };
        });

        console.log(combinedResults)
        return res.json(combinedResults);
    } catch (error) {
        console.error("Error fetching user friends: ", error);
        res.status(500).json({ error: 'Internal server error' });
    }
    
});

module.exports = router;
