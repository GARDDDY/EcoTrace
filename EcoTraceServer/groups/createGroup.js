const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { checkOAuth2 } = require('../tech/oauth');
const connections = require("../server");
const connection1 = connections["users"];
const connection2 = connections["groups"];
const router = express.Router();

const { v4: uuidv4 } = require('uuid');

const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, './EcoTraceServer/uploads/groups');
    },
    filename: function (req, file, cb) {
        cb(null, file.originalname);
    }
});

const upload = multer({ storage: storage });

router.post('/createGroup', upload.single('image'), async (req, res) => { // todo
    const data = JSON.parse(req.body.jsonData);
    const userId = req.query.cuid || "0";
    const oauth = req.query.oauth || "0";

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    console.log(data)

    const group = data[0];
    const rulesText = data[1];

    const [exp] = await connection1.execute("select experience from user where userId = ?", [userId])

    if (exp[0].experience < 100) {
        return res.status(403).send(["Получите 100 очков опыта, что создавать свои группы!"])
    }

    const gid = group.groupId || uuidv4();

    const tempPath = path.join(__dirname, '../uploads/groups', req.file.originalname);
    const newPath = path.join(__dirname, '../uploads/groups', gid + path.extname(req.file.originalname));

    fs.rename(tempPath, newPath, (err) => {
        if (err) {
            console.error('Error renaming file:', err);
            return res.status(500).json({ error: 'Failed to rename file' });
        }
        console.log('File renamed successfully');
    });
    
    const [existingGroup] = await connection2.execute("SELECT * FROM `group` WHERE groupId = ?", [gid]);

    if (existingGroup.length > 0) {
        console.log("updating!") // todo conditions on null value
        await connection2.execute("UPDATE `group` SET groupName = ?, groupCreatorId = ?, filters = ?, groupType = ?, groupAbout = ?, groupRules = ? WHERE groupId = ?",
            [group.groupName, userId, group.filters || null, group.groupType || 0, group.groupAbout || null, rulesText, gid]
        );
    } else {
        const [anyName] = await connection2.execute('select groupId from `group` where groupName = ? and groupId != ?', [group.groupName, gid])

        if (anyName.length > 0) {
            return res.send(["Группа с таким именем уже существует!"])
        }

        await connection2.execute("INSERT INTO `group` (groupId, groupName, groupCreatorId, filters, groupType, groupAbout, groupRules) VALUES (?, ?, ?, ?, ?, ?, ?)",
            [gid, group.groupName, userId, group.filters || null, group.groupType || 0, group.groupAbout || null, rulesText || null]
        );

        await connection1.execute("insert into `groups` (userId, groupId, role) values (?,?,?)", [userId, gid, 0])
    }


    res.status(200).send([null]);
});

module.exports = router;
