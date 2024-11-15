const express = require('express');

const connections = require("../server")
const connection1 = connections["auth"]
const connection2 = connections["users"]

const router = express.Router();
const {crypt, uncrypt, gut, verify} = require("./tAuth")



router.get('/gat', async (req, res) => { // generate auth token by user token
    const usertkn = req.query.tkn;

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    try {

        const data = uncrypt(usertkn);
        
        const payload = {
            type: 1, // auth
            uid: data.uid,
            given: Date.now(),
            expires: 1 * 60 * 60 * 1000, // hours
        }

        return res.json([crypt(JSON.stringify(payload))]);
    } catch (error) {
        return res.json([null])
    }
});

router.get('/login', async (req, res) => { // generate auth token by user token
    const email = req.query.email;
    const password = req.query.password;

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const [uid] = await connection2.execute("select userId from user where email = ? and password = ?", [email, password])

    if (uid.length == 0) {
        return res.send(null)
    }

    const [user] = await connection1.execute("select userToken from sessions where userId = ?", [uid[0].userId])

    return res.send([uid[0].userId, user[0].userToken]);
});

const serviceMail = {
    "service": process.env.EMAIL_SERVICE,
    "auth": {
        "user": process.env.EMAIL,
        "pass": process.env.EMAIL_PASSWORD
    }
}
const nodemailer = require('nodemailer');
router.get('/sendCode', async (req, res) => {
    const email = req.query.email;

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const [user] = await connection2.execute("select fullname, username from user where email = ?", [email])

    if (user.length == 0) {
        return res.send([false])
    }

    var username = user[0].fullname
    if (username === null) {
        username = user[0].username
    }

    const resetCode = Math.floor(100000 + Math.random() * 900000);

    const mailOptions = {
        from: "EcoTrace",
        to: email,
        subject: 'Сброс пароля',
        html: `<!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: Arial, sans-serif;
                    background-color: #f4f4f4;
                    margin: 0;
                    padding: 20px;
                }
                .container {
                    max-width: 600px;
                    margin: auto;
                    background: #fff;
                    padding: 20px;
                    border-radius: 5px;
                    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                }
                .header {
                    text-align: center;
                    margin-bottom: 20px;
                }
                .code {
                    font-size: 24px;
                    font-weight: bold;
                    color: #25633B;
                    text-align: center;
                    display: block;
                    margin: 20px 0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>Здравствуйте, ${username}!</h2>
                </div>
                <p>Используйте этот код в приложении EcoTrace для сброса пароля своей учетной записи!</p>
                <p class="code">${resetCode}</p>
            </div>
        </body>
        </html>`,
    };
    const transporter = nodemailer.createTransport(serviceMail);
        transporter.sendMail(mailOptions, async (error, info) => {
            if (error) {
                return res.status(500).send([false]);
            }
            
            try {
                await connection1.execute(
                    "INSERT INTO codes (email, code, expires, type) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE code = ?, expires = ?, applied = ?",
                    [email, resetCode, new Date(Date.now() + 300 * 1000), 0, resetCode, new Date(Date.now() + 300 * 1000), null]
                );
                return res.send([true]);
            } catch (dbError) {
                return res.status(500).send([false, dbError.message]);
            }
        });
        
});

router.get('/applyCode', async (req, res) => {
    const email = req.query.email;
    const code = req.query.code;

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const currTime = Date.now();
    const [c] = await connection1.execute("SELECT * FROM codes WHERE email = ? AND code = ? AND expires >= ? AND applied IS NULL", [email, code, currTime]);
    console.log(c)
    if (c.length !== 0) {
        await connection1.execute("UPDATE codes SET applied = ? WHERE email = ? AND code = ? AND expires >= ? AND applied IS NULL", [currTime, email, code, currTime]);
        return res.send([true]);
    } else {
        return res.send([false]);
    }
});

router.get('/changePassword', async (req, res) => {
    const email = req.query.email;
    const password = req.query.password;
    const code = req.query.code;

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const currTime = Date.now();
    const [c] = await connection1.execute("SELECT * FROM codes WHERE email = ? AND code = ? AND expires >= ?", [email, code, currTime]);

    if (c.length > 0) {
        await connection2.execute("UPDATE user SET password = ? WHERE email = ?", [password, email]);
        return res.send([true]);
    } else {
        return res.send([false]);
    }
});


module.exports = router
