const express = require('express');
const path = require('path');
const app = express();
const port = 35288
const mysql = require("mysql2/promise")


const dbs = ["users", "events", "groups", "web", "calculator", "auth"];
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

    app.use(require("./tech/Auth"));



    app.use(require("./app/constants"));
    app.use(require("./app/web"));
    app.use(require("./app/getWeb"));
    app.use(require("./app/getRating"));



    app.use(require("./groups/observePosts"));

    app.use(require("./user/getUpdates"));
    app.use(require("./user/getEducations"));

    app.use(require("./user/getEduStatus"));
    app.use(require("./user/applyEdu"));

    app.use(require("./user/getUser"));
    app.use(require("./user/getGraph"));
    app.use(require("./user/getEvents"));
    app.use(require("./user/getFriends"));
    app.use(require("./user/areUsersFriends"));
    app.use(require("./user/addFriend"));
    app.use(require("./user/removeFriend"));
    app.use(require("./user/getGroups"));

    app.use(require("./user/getEcoCalc"));
    app.use(require("./user/setEcoData"));
    app.use(require("./user/calcResults"));

    app.use(require("./user/getUserEmail"));
    app.use(require("./user/getRules"));
    app.use(require("./user/register"));
    app.use(require("./user/set"));
    app.use(require("./user/getPrivate"));

    app.use(require("./user/getAllUsers"));
    
    

    app.use(require("./event/getEvent"));
    app.use(require("./event/createEvent"));
    app.use(require("./event/getGoals"));
    app.use(require("./event/getTimes"));
    app.use(require("./event/getCoords"));
    app.use(require("./event/getMembers"));

    app.use(require("./event/joinEvent"));
    app.use(require("./event/leaveEvent"));

    app.use(require("./event/deleteEvent")); // ?
    app.use(require("./event/deleteEvent")); // ?

    app.use(require("./event/getUserEventData"));

    app.use(require("./event/isUserModerInEvent"));

    app.use(require("./event/isUserValidated"));
    app.use(require("./event/validateUser"));
    app.use(require("./event/setEventRole"));

    app.use(require("./event/getAllEvents"));



    app.use(require("./groups/getGroup"));
    app.use(require("./groups/isGroupNameAvailable"));
    app.use(require("./groups/createGroup"));
    app.use(require("./groups/deleteGroup"));
    app.use(require("./groups/getGroupMembers"));
    app.use(require("./groups/getGroupRules"));

    app.use(require("./groups/isUserInGroup"));
    app.use(require("./groups/getUserRoleInGroup"));

    app.use(require("./groups/joinGroup"));
    app.use(require("./groups/leaveGroup"));

    app.use(require("./groups/setUserRoleInGroup"));
    app.use(require("./groups/kickUserFromGroup"));

    app.use(require("./groups/getPosts"));
    app.use(require("./groups/createPost"));
    app.use(require("./groups/deletePost"));
    
    app.use(require("./groups/getComments"));
    app.use(require("./groups/createComment"));
    app.use(require("./groups/deleteComment"));
    
    app.use(require("./groups/getAllGroups"));
   


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
