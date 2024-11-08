const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { checkOAuth2 } = require('../tech/oauth');

const multer = require('multer');
const path = require('path');
const fs = require('fs');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

const storage = multer.diskStorage({
  destination: function (req, file, cb) {
      cb(null, './EcoTraceServer/uploads/events');
  },
  filename: function (req, file, cb) {
      cb(null, file.originalname);
  }
});

const upload = multer({ storage: storage });

router.post('/createEvent', upload.single('image'), async (req, res) => { // check todo
    const data = JSON.parse(req.body.jsonData);
    const userId = req.query.cuid || '0';
    const oAuth = req.query.oauth || '0';

    console.log(data)

    if (!connection2 || !connection1) {
        console.error("not connected to events")
        return
    }

    if (!await checkOAuth2(oAuth, userId)) {
        return res.send([null])
    }


    const [userExp] = await connection1.execute('select experience from user where userId = ?', [userId])

    if (userExp[0].experience < 50) {
        return res.send([null])
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const eid = uuidv4();
    try {

      const tempPath = path.join(__dirname, '../uploads/events', req.file.originalname);
      const newPath = path.join(__dirname, '../uploads/events', eid + path.extname(req.file.originalname));

      fs.rename(tempPath, newPath, (err) => {
          if (err) {
              console.error('Error renaming file:', err);
          }
          console.log('File renamed successfully');
      });
    } catch (e) {
      console.log("bad image", e)
    }
    
    await connection2.execute(
        `INSERT INTO event (eventId, eventName, eventCountMembers, eventCreatorId, eventStatus, filters) 
            VALUES (?, ?, ?, ?, ?, ?)`,
            [eid,data[0].eventName, 1, userId, 0, data[0].filters]
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

    await connection1.execute(`INSERT INTO events (userId, eventId, eventRole, validated) VALUES (?, ?, ?, ?)`, 
        [userId, eid, 0, 1])

    res.send([eid]);
});

module.exports = router;
