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
        cb(null, './EcoTraceServer/uploads/posts');
    },
    filename: function (req, file, cb) {
        cb(null, Date.now() + path.extname(file.originalname));
    }
});

const upload = multer({ storage: storage });

router.post('/sendComment', upload.single('image'), async (req, res) => {
    const data = JSON.parse(req.body.jsonData);
    const image = req.files;
    const groupId = req.query.gid || '0';
    const postId = req.query.postId || '0';
    const userId = req.query.cuid || "0";
    const oauth = req.query.oauth || "0";

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed ev" });
    }

    console.log(data)

    const imgName = req.file ? req.file.filename : null;
    
    const [isUserInGroup] = await connection1.execute("SELECT * FROM `groups` WHERE groupId = ? and userId = ?", [groupId, userId]);

    if (isUserInGroup.length === 0) {
        console.log("not in group")
        return res.send([false])
    } else {
        const [isPostAvailbale] = await connection2.execute('select * from posts where groupId = ? and postId = ?', [groupId, postId])

        if (isPostAvailbale.length === 0) {
            console.log("no post")
            return res.send([false])
        }

        await connection2.execute('insert into comments (groupId, postId, commentCreatorId, commentContentText, commentContentImage, commentTime) values(?,?,?,?,?,?)',
            [groupId, postId, userId, data[0], path.basename(imgName ? imgName : "", path.extname(imgName ? imgName : "")), Math.floor(Date.now()/1000)]
        )

        return res.send([true])
    }
});

module.exports = router;
