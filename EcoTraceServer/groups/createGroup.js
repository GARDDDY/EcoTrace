const express = require('express');
const multer = require('multer');
const path = require('path');
const connections = require("../server");
const connection1 = connections["users"];
const connection2 = connections["groups"];
const router = express.Router();

const { v4: uuidv4 } = require('uuid');

const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, './EcoTraceServer/groups/uploads');
    },
    filename: function (req, file, cb) {
        cb(null, Date.now() + path.extname(file.originalname));
    }
});

const upload = multer({ storage: storage });

router.post('/createGroup', upload.single('image'), async (req, res) => {
    const data = JSON.parse(req.body.jsonData);
    const image = req.files;
    const userId = req.query.cuid || "0";
    const oauth = req.query.oauth || "0";

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    const group = data[0];
    const rulesText = data[1];

    const [exp] = await connection1.execute("select experience from user where userId = ?", [userId])

    if (exp[0].experience < 100) {
        return res.status(403).send(["Получите 100 очков опыта, что создавать свои группы!"])
    }

    // todo check the name validity
    const gid = group.groupId === "0" ? uuidv4() : group.groupId;
    const imgName = req.file ? req.file.filename : null;
    
    const [existingGroup] = await connection2.execute("SELECT * FROM `group` WHERE groupId = ?", [gid]);

    if (existingGroup.length > 0) {
        await connection2.execute("UPDATE `group` SET groupName = ?, groupCreatorId = ?, filters = ?, groupType = ?, groupAbout = ?, groupRules = ?, groupRulesImage = ? WHERE groupId = ?",
            [group.groupName, userId, group.filters, group.groupType, group.groupAbout, rulesText, imgName, gid]
        );
    } else {
        await connection2.execute("INSERT INTO `group` (groupId, groupName, groupCreatorId, filters, groupType, groupAbout, groupRules, groupRulesImage) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            [gid, group.groupName, userId, group.filters, group.groupType, group.groupAbout, rulesText, imgName]
        );
    }

    await connection1.execute("insert into `groups` (userId, groupId, role) values (?,?,?)", [userId, gid, 0])

    res.status(200).send([null]);
});

module.exports = router;
