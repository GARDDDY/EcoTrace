const express = require('express');
const admin = require('firebase-admin');
const path = require('path');
const app = express();
const port = 8000; // 35288
const mysql = require("mysql2/promise")

const serviceAccount = require('./config/ecotrace-cf2be-firebase-adminsdk-manyb-f71a3569d6.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://ecotrace-cf2be-default-rtdb.firebaseio.com/"
});

const dbs = ["users", "events", "groups", "web", "calculator"];
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

    app.use(require("./groups/observePosts"));

    app.use(require("./app/web"));
    app.use(require("./tech/constants"));
    app.use(require("./app/getWeb"));
    app.use(require("./app/getRating"));

    app.use(require("./user/getUser"));
    app.use(require("./user/getGraph"));
    app.use(require("./user/getUpdates"));
    app.use(require("./user/getEducations"));
    app.use(require("./user/applyEdu"));
    app.use(require("./user/getAllUsers"));
    app.use(require("./user/getEcoCalc"));
    app.use(require("./user/calcResults"));
    app.use(require("./user/getFriends"));
    app.use(require("./user/getGroups"));
    app.use(require("./user/getEvents"));
    app.use(require("./user/setEcoData"));
    app.use(require("./user/getUserEmail"));

    app.use(require("./event/createEvent"));
    app.use(require("./event/getGoals"));
    app.use(require("./event/getTimes"));
    app.use(require("./event/getCoords"));
    app.use(require("./event/getMembers"));
    app.use(require("./event/getAllEvents"));
    app.use(require("./event/getUserEventData"));
    app.use(require("./event/isUserModerInEvent"));
    app.use(require("./event/validateUser"));
    app.use(require("./event/isUserValidated"));

    app.use(require("./groups/getAllGroups"));
    app.use(require("./groups/createGroup"));
    app.use(require("./groups/getGroup"));
    app.use(require("./groups/getPosts"));
    app.use(require("./groups/getComments"));
    app.use(require("./groups/createPost"));
    app.use(require("./groups/isUserInGroup"));
    app.use(require("./groups/getGroupMembers"));
    app.use(require("./groups/setUserRoleInGroup"));
    app.use(require("./groups/kickUserFromGroup"));
    app.use(require("./groups/getUserRoleInGroup"));
    app.use(require("./groups/isGroupNameAvailable"));

    app.get('/uploads/:dir/:filename', (req, res) => {
        const imagePath = req.params.dir+'/'+req.params.filename + '.png'
        console.info("Request on image", imagePath)
        res.sendFile(path.join(__dirname, 'uploads', imagePath));
    });
    

    app.listen(port, () => {
        console.log(`Server is running at http://localhost:${port}`);
    });
}).catch((err) => {
    console.error('Error starting server:', err);
});

module.exports = dbConnections;
