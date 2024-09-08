const admin = require('firebase-admin');

async function getUsernameOnly(userId) {
    try {
        const reference = admin.database().ref('users/' + userId + '/username/');
        const snapshot = await reference.once('value');
        const value = snapshot.val() || "";
        return value;
    } catch (error) {
        console.log(error);
        return "";
    }
}

module.exports = { getUsernameOnly }