const express = require('express');
const admin = require('firebase-admin');
// const { checkOAuth2 } = require('../tech/oauth');
// const { getRules } = require('../tech/getUserRules');
// const { areUsersFriends } = require('../tech/areUsersFriends');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["events"]

const router = express.Router();

router.get('/getUserEvents', async (req, res) => {
  const userId = req.query.uid || '0';
  let block = req.query.block || null;
  const oAuth = req.query.oauth || '0';
  const requestFrom = req.query.cid || '0';

  if (!connection1) {
    console.error("not connected to events")
    return
}

  res.setHeader('Content-Type', 'application/json; charset=utf-8');

  // if (!await checkOAuth2(oAuth, requestFrom)) {
  //     return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
  // }

  const getEvents = async (block, userId, requestFrom, dataGlobal = [], repeated = false) => {
    let query = `SELECT * FROM events WHERE userId = "${userId}" AND (eventId > "${block}" OR ${block} IS NULL) ORDER BY eventId LIMIT 2`;

    try {
      const [rows] = await connection1.execute(query);

      console.log(rows)

      
      const resultObject = rows.reduce((obj, row) => {
        obj[row.eventId] = row;
        return obj;
      }, {});

      const lastEventId = rows.length > 0 ? rows[rows.length - 1].eventId : null;

      if (repeated || rows.length >= 2) {
       
        res.json(Object.keys(resultObject).length === 0 ? null : resultObject);
      } else {
        
        dataGlobal.push(...rows);
        await getEvents(lastEventId, userId, requestFrom, dataGlobal, true);
      }
    } catch (error) {
      console.error('Error fetching events:', error);
      res.status(500).json({ error: 'Internal Server Error' });
    }
  };

  await getEvents(block, userId, requestFrom);
});

module.exports = router;
