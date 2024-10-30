const express = require('express');
const { v4: uuidv4 } = require('uuid');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.post('/createEvent', async (req, res) => {
    const data = req.body;
    const userId = req.query.cuid || '0';
    const oAuth = req.query.oauth || '0';

    console.log(data)

    if (!connection2 || !connection1) {
        console.error("not connected to events")
        return
    }

    // todo make exp (50?) check, oauth

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const eid = uuidv4();
    
    await connection2.execute(
        `INSERT INTO event (eventId, eventName, eventCountMembers, eventCreatorId, eventStatus, filters, eventStart) 
            VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [               eid,     data[0].eventName, 1,             userId, 0,     data[0].filters, data[0].eventStart]
          );
    
          try {
            for (const [index, content] of Object.entries(data[1])) {
              await connection2.execute(
                `INSERT INTO eventtimes (eventId, timeId, timeName) 
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                timeName = VALUES(timeName);`,
                [eid, index, content]
              );
            }
        
            for (const [index, content] of Object.entries(data[2])) {
              await connection2.execute(
                `INSERT INTO eventgoals (eventId, goalId, goalText) 
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                goalText = VALUES(goalText);`,
                [eid, index, content]
              );
            }
    
            for (const [index, [_, content]] of Object.entries(data[3]).entries()) {
              await connection2.execute(
                `INSERT INTO eventcoords (eventId, coordId, latitude, longitude, coordText, coordType, coordRadius, coordColor1, coordColor2, relationWithTime) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
                [eid, index, content.objectCenter.latitude, content.objectCenter.longitude, content.objectName, content.objectType, content.circleRadius || null, content.fillColor || null, content.strokeColor || null, content.objectRelation || null]
              );
            }
        
            console.log('Data inserted successfully');
          } catch (error) {
            console.error('Error inserting data:', error);
          }

    await connection1.execute(`INSERT INTO events (userId, eventId, eventRole) VALUES (?, ?, ?)`, 
        [userId, eid, 0])

    res.json(eid);
});

module.exports = router;
