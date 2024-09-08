const express = require('express');
const admin = require('firebase-admin');

const router = express.Router();

router.get('/getUserEmail', async (req, res) => {
const login = req.query.lgn || '0';
    const password = req.query.pss || '0';

    try {
        const userIdRef = admin.database().ref(`indexes/${login}`);
        const userIdSnapshot = await userIdRef.once('value');
        
        if (!userIdSnapshot.exists()) {
            return res.send(null);
        }

        const userId = userIdSnapshot.val();
        const userRef = admin.database().ref(`users/${userId}/private`);
        const userSnapshot = await userRef.once('value');
        const user = userSnapshot.val();

        if (user && user.password === password) {
            return res.send(user.email);
        } else {
            return res.send(null);
        }
    } catch (error) {
        console.error('Error fetching user data:', error);
        return res.status(500).send('Internal Server Error');
    }
});

module.exports = router;