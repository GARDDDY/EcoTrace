const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { checkOAuth2 } = require('../tech/oauth');

const t = require("../tech/tAuth")

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["calculator"]
const connection3 = connections["auth"]

const router = express.Router();

const multer = require('multer');
const path = require('path');
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, './EcoTraceServer/uploads/users');
    },
    filename: function (req, file, cb) {
        const name = req.query.cuid || file.originalname;
        const ext = path.extname(file.originalname);
        cb(null, `${name}${ext}`);
    }
});

const upload = multer({ storage: storage });

function genId() {
    const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
    let id = '';
    var num = Date.now()
    while (num > 0) {
        id = chars[num % 62] + id;
        num = Math.floor(num / 62);
    }
    return id;
}



// change email
router.post('/setm', upload.single('image'), async (req, res) => {
    const userData = JSON.parse(req.body.jsonData);
    const userId = req.query.cuid || '0'
    const oauth = req.query.oauth || '0'

    if (!await checkOAuth2(oauth, userId)) {
        return res.send([false])
    }

    const [e] = await connection1.execute('select email from user where userId = ?', [userId])

    if (e[0].email == userData[0]) {
        await connection1.execute('update user set email = ? where userId = ?', [userData[1], userId])
        return res.send([true])
    } else {
        return req.send([false])
    }



});

const crypto = require("crypto")
function sha256(message) {
    return crypto.createHash('sha256').update(message).digest('hex');
}

router.get("/cm", async (req, res) => {
    const userId = req.query.cid || '0'
    const oauth = req.query.oauth || '0'
    const email = req.query.e || null;

    if (!await checkOAuth2(oauth, userId)) {
        return res.send([false])
    }

    const [e] = await connection1.execute('select email from user where userId = ?', [userId])

    console.log(email, e)

    return res.send([email === sha256(e[0].email)])
})


// change password
router.post('/setp', upload.single('image'), async (req, res) => {
    const userData = JSON.parse(req.body.jsonData);
    const userId = req.query.cuid || '0'
    const oauth = req.query.oauth || '0'

    if (!await checkOAuth2(oauth, userId)) {
        return res.send([false])
    }

    const password1 = userData[0];
    const password2 = userData[1];


    const [u] = await connection1.execute("select password from user where userId = ?", [userId])
    
    if ([u].password === password1) {
        await connection1.execute('update user set password = ? where userId = ?', [password2, userId])
        return res.send([true])
    } else {
        return res.send([false])
    }
});





router.post('/set', upload.single('image'), async (req, res) => {
    const userData = JSON.parse(req.body.jsonData);
    const userId = req.query.cuid || '0'
    const oauth = req.query.oauth || '0'

    if (!await checkOAuth2(oauth, userId)) {
        return res.send([false])
    }

    console.log(userId, userData)

    try {
        await connection1.execute(`update user set 
            country_code = case when ? is null then country_code else ? end, 
            username = case when ? is null then username else ? end,
            about_me = case when ? is null then about_me else ? end,
            filters = case when ? is null then filters else ? end,
            fullname = case when ? is null then fullname else ? end
            where userId = ?`, [
                userData.country_code || null, userData.country_code || null, 
                userData.username || null, userData.username || null, 
                userData.about_me || null, userData.about_me || null, 
                userData.filters || null, userData.filters || null, 
                userData.fullname || null, userData.fullname || null, 
                userId
            ])

            return res.send([true])
    } catch (e) {
        console.error(e)
        return res.send([false])
    }



});
router.post('/setr', upload.single('image'), async (req, res) => {
    const userData = JSON.parse(req.body.jsonData);
    const userId = req.query.cuid || '0'
    const oauth = req.query.oauth || '0'

    if (!await checkOAuth2(oauth, userId)) {
        return res.send([false])
    }

    console.log(userId, userData)

    await connection1.execute(`update rules set canSeeCountry = ?, canSeeFullname = ?, canSeeFriends = ?, canSeeGroups = ? where userId = ?`, [
        userData.canSeeCountry, userData.canSeeFullname, userData.canSeeFriends, userData.canSeeGroups, userId]) 

    res.send([true])

});


router.post('/seta', upload.single('image'), async (req, res) => {
    const userId = req.query.cuid || '0'
    const oauth = req.query.oauth || '0'

    if (!await checkOAuth2(oauth, userId)) {
        return res.status(403).send([false])
    }

    res.send([true])
    
});




module.exports = router;