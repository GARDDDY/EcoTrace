const admin = require('firebase-admin');
const {crypt, uncrypt, gut, verify} = require("./tAuth") 

async function checkOAuth2(token, userId) {
    // if (!token || !userId) {
    //     throw new Error('Bad data');
    // }

    // try {
    //     const auth = admin.auth();
    //     const verifiedIdToken = await auth.verifyIdToken(token);
    //     console.log("VErifying user", verifiedIdToken.sub, userId)
    //     return verifiedIdToken.sub === userId;
    // } catch (error) {
    //     console.error("Verify Error: ", error);
    //     return false;
    // }
    return await verify(userId, token)
}

module.exports = { checkOAuth2 };