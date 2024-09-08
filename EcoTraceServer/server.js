const express = require('express');
const admin = require('firebase-admin');
const path = require('path');
const app = express();
const port = 8000;
const mysql = require("mysql2/promise")

const serviceAccount = require('./ecotrace-cf2be-firebase-adminsdk-manyb-ad21b70b6a.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://ecotrace-cf2be-default-rtdb.firebaseio.com/"
});


const dbs = ["users", "events"]//, "groups", "calculator", "devices"];
const dbConnections = {};

async function connectToDatabases() {
  const connectionPromises = dbs.map(async (db) => {
    try {
      const connection = await mysql.createConnection({
        host: "127.0.0.1",
        port: 3306,
        user: "root",
        database: db,
        password: "RussiaZV"
      });
      console.info(`Connected to ${db.toUpperCase()}, ID-${connection.threadId}`);
      dbConnections[db] = connection;
    } catch (err) {
      console.error(`Unable to connect to ${db.toUpperCase()}:`, err);
      throw err;
    }
  });

  await Promise.all(connectionPromises);
}

connectToDatabases().then(() => {
  app.use(express.json());
  app.use(require("./user/getUser"));
  app.use(require("./user/getGraph"));
  app.use(require("./event/getEvents"));
  app.use(require("./event/getGoals"));
  app.use(require("./event/getTimes"));
  app.use(require("./event/getCoords"));
  app.use(require("./event/getMembers"));
  app.use(require("./event/getAllEvents"));
  app.use(require("./event/getUserEventData"));
  app.use(require("./event/isUserModerInEvent"));
  app.use(require("./user/getFriends"));
  app.use(require("./user/getGroups"));
  app.use(require("./user/setEcoData"));
  app.use(require("./app/getRating"));
  app.use(require("./tech/getUserEmail"));
  app.use(require("./app/getObjectsFilterByFiltersAndName"));

  app.listen(port, () => {
    console.log(`Server is running at http://localhost:${port}`);
  });
}).catch((err) => {
  console.error('Error starting server:', err);
});

module.exports = dbConnections;

