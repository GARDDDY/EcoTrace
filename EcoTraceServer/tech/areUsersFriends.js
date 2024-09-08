const admin = require('firebase-admin');

async function areUsersFriends(user1, user2) {
    try {
        const reference = admin.database().ref('users/' + user1 + '/friends/' + user2 + "friend");
        const snapshot = await reference.once('value');
        const value = snapshot.val() || false;
        return value;
    } catch (error) {
        return false;
    }
}

module.exports = {
    areUsersFriends
}