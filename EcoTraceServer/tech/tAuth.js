const connections = require("../server")
const connection1 = connections["auth"]

async function gut(userId, password) { // return user token

    const [tkn] = await connection1.execute("select userToken from sessions where userId = ?", [userId])

    if (tkn.length == 0) {

        const payload = {
            type: 0, // user
            regTime: Date.now(),
            uid: userId,
            // pass: password,
        };

        const token = crypt(JSON.stringify(payload));

        await connection1.execute("insert into sessions (userId, regTime, userToken) values(?,?,?)", [userId, payload.regTime, token])
        return token
        
    } else {
        return tkn[0].userToken
    }
}


function verify(userId, token) { // auth token verification
    let data;

    try {
        data = uncrypt(token);
    } catch (error) {
        return false;
    }

    if (data.type === 1) {
        const currTime = Date.now();

        if (data.given + data.expires > currTime) {
            return userId === data.uid;
        }
    }

    return false;
}



const crypto = require('crypto');

const secretKey = '56928b0a7cebd6cf77389ab36154843efe374ea2525d9f0535fabf07eac83fc3'; // secret tsss!!!
const algorithm = 'aes-256-cbc';

function crypt(text) {
    const iv = crypto.randomBytes(16);
    const cipher = crypto.createCipheriv(algorithm, Buffer.from(secretKey, 'hex'), iv);
    let encrypted = cipher.update(text, 'utf8', 'hex');
    encrypted += cipher.final('hex');

    return `${iv.toString('hex')}:${encrypted}`;
}

function uncrypt(encryptedText) {
    const parts = encryptedText.split(':');
    const iv = Buffer.from(parts.shift(), 'hex'); 
    const encryptedTextBuffer = Buffer.from(parts.join(':'), 'hex');
    
    const decipher = crypto.createDecipheriv(algorithm, Buffer.from(secretKey, 'hex'), iv);
    let decrypted = decipher.update(encryptedTextBuffer, 'hex', 'utf8');
    decrypted += decipher.final('utf8');

    return JSON.parse(decrypted);
}


module.exports = {crypt, uncrypt, gut, verify}