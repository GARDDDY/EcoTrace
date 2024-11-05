const express = require('express');
const multer = require('multer');
const path = require('path');
const { checkOAuth2 } = require('../tech/oauth');
const connections = require("../server");
const connection1 = connections["users"];
const connection2 = connections["groups"];
const router = express.Router();

const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, './EcoTraceServer/uploads');
    },
    filename: function (req, file, cb) {
        cb(null, Date.now() + path.extname(file.originalname));
    }
});

const upload = multer({ storage: storage });

router.post('/createPost', upload.single('image'), async (req, res) => {
    const groupId = req.query.gid || '0';
    const text = req.body.jsonData;
    const image = req.files;
    const userId = req.query.cuid || "0";
    const oauth = req.query.oauth || "0";

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }


    const [userInGroup] = await connection1.execute('SELECT * FROM `groups` WHERE userId = ? and groupId = ?', [userId, groupId]);

    if (userInGroup.length === 0) {
        console.log("cause not in group", userId)
        return res.status(403).send([false]);
    }

    const time = parseInt(new Date().getTime() / 1000);
    const textString = JSON.parse(text)[0];

    const imgName = req.file ? req.file.filename : null;
    console.log("IMG", imgName)

    await connection2.execute('insert into `posts` (groupId, postTime, postCreatorId, postContentText, postContentImage) values (?, ?, ?, ?, ?)',
        [groupId, time, userId, textString, imgName]
    )
    
    

    console.log(groupId, textString)
    console.warn(image)

    res.status(200).send([true]);
});

module.exports = router;
