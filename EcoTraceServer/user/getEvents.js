const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.get('/getUserEvents', async (req, res) => {
    const userId = req.query.uid || '0';
    const block = req.query.block || null;
    const sort = req.query.sort || '-1';

    const orderBy = sort === '0' ? 'eventCountMembers DESC' : 'e.eventRole ASC';
    console.log(orderBy)
    const [events] = await connection1.execute(`
        SELECT 
            e.eventId, 
            e.eventRole, 
            e.validated, 
            (SELECT COUNT(*) FROM events AS sub WHERE sub.eventId = e.eventId) AS eventCountMembers
        FROM events AS e
        WHERE e.userId = ? AND (? IS NULL OR e.eventId > ?)
        ORDER BY ${orderBy} 
        LIMIT 3
    `, [userId, block, block]);

    console.log(events)

    const eventsIds = events.map(event => event.eventId);
    const eventsId = eventsIds.map(id => `'${id}'`).join(', ');

    let eventsData = [];
    if (eventsId.length > 0) {
        const [results] = await connection2.execute(`
            SELECT eventId, eventName, eventStatus
            FROM event 
            WHERE eventId IN (${eventsId})
        `);
        
        eventsData = results;
    }

    const combinedResults = events.map(event => {
        const relatedEvent = eventsData.find(data => data.eventId === event.eventId);
        return {
            isValidated: Boolean(event.validated),
            eventRole: event.eventRole,
            eventInfo: {
                eventId: event.eventId,
                eventName: relatedEvent ? relatedEvent.eventName : null,
                eventStatus: relatedEvent ? relatedEvent.eventStatus : null,
                eventCountMembers: event.eventCountMembers
            }
        };
    });

    res.json(combinedResults)
    
});

module.exports = router;
