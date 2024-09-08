const admin = require('firebase-admin');

async function checkOAuth2(token, userId) {
    if (!token || !userId) {
        throw new Error('Bad data');
    }

    try {
        const auth = admin.auth();
        const verifiedIdToken = await auth.verifyIdToken(token);
        return verifiedIdToken.sub === userId;
    } catch (error) {
        console.error("Verify Error: ", error);
        return false;
    }
}

module.exports = { checkOAuth2 };