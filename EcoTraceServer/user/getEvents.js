const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

function formatDate(timeString) {
    const date = new Date(timeString*1000);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}.${month}.${year}`;
}


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

    

    const eventsIds = events.map(event => event.eventId);
    const eventsId = eventsIds.map(id => `'${id}'`).join(', ');

    let eventsData = [];
    if (eventsId.length > 0) {
        const [results] = await connection2.execute(`
            SELECT e.eventId, e.eventName, e.eventStatus, 
                (SELECT MIN(SUBSTRING_INDEX(timeId, '_', 1)) FROM eventtimes WHERE eventId IN (${eventsId})) AS minTime,
                (SELECT MAX(SUBSTRING_INDEX(timeId, '_', -1)) FROM eventtimes WHERE eventId IN (${eventsId})) AS maxTime
                FROM event e
                WHERE e.eventId IN (${eventsId})
        `);
        
        console.log("event", results)
        eventsData = results;
    }

    const currentTime = Date.now()/1000;
    const combinedResults = events.map(event => {
        const relatedEvent = eventsData.find(data => data.eventId === event.eventId);
        return {
            isValidated: Boolean(event.validated),
            eventRole: event.eventRole,
            eventInfo: {
                eventId: event.eventId,
                eventName: relatedEvent ? relatedEvent.eventName : null,
                eventStatus: relatedEvent ? relatedEvent.minTime <= currentTime && currentTime <= relatedEvent.maxTime ? 1 : currentTime < relatedEvent.minTime ? 0 : 2 : 0,
                eventStatusString: relatedEvent
                ? relatedEvent.minTime <= currentTime && currentTime <= relatedEvent.maxTime
                    ? "Проходит"
                    : currentTime < relatedEvent.minTime
                    ? `Начнется ${formatDate(relatedEvent.minTime)}`
                    : `Закончилось ${formatDate(relatedEvent.maxTime)}`
                : "???",
                eventCountMembers: event.eventCountMembers
            }
        };
    });

    res.json(combinedResults)
    
});

module.exports = router;
