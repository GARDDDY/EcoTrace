const admin = require('firebase-admin');
const { getUsernameOnly } = require('../tech/getUsernameOnly');

async function sendMsg(uid, msgData) {
    try {
        const tokensSnapshot = await admin.firestore()
            .collection('users')
            .doc(uid)
            .collection('notifications')
            .get();
        
        const tokens = tokensSnapshot.docs.map(doc => doc.data().token);

        if (tokens.length === 0) {
            return res.status(404).send('No tokens found for the user.');
        }

        const message = {
            data: {
                title: msgData.title,//'Запрос на добавление в друзья',
                body: msgData.body,//`${senderName} хочет с Вами дружить!`,
                image: msgData.imgURL,//'https://example.com/image.png',
                clickAction: msgData.action,//'OPEN_PROFILE',
                // sound: 'default',
                // color: '#85ff85',

                data: msgData.data
            },
            tokens: tokens
        };

        const response = await admin.messaging().sendMulticast(message);

        console.log('Successfully sent messages:', response);
        res.status(200).send('Messages sent successfully');
        
    } catch (error) {
        console.error('Unsuccessfully sent messages:', error);
        res.status(500).send('Messages sent unsuccessfully');
    }
}

module.exports = {
    sendMsg
}
