const express = require('express');
const admin = require('firebase-admin');
const { checkOAuth2 } = require('../tech/oauth');

const router = express.Router();

function dateToDataFormat(date) {
    const dateList = date.split('.').map(String)
    return dateList[2]
        +dateList[1].padStart(2, "0")
        +dateList[0].padStart(2, "0");
} 

function newAverage(plus, previous, count) {
    return (previous + plus) / (count + 1);
}

router.post('/setEcoData', async (req, res) => {
    const data = req.body;
    const calcType = req.query.cType || 0;
    const userId = req.query.cuid || '0';
    const oAuth = req.query.oauth || '0';

    if (!await checkOAuth2(oAuth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }

    

    const refernceSaveData = admin.database().ref("calculator/"+userId+"/data/dynamic/type"+calcType);
    const snapshotSaveData = await refernceSaveData.once('value');
    const values = snapshotSaveData.val() || {};

    for (q = 0; q < data.length; ++q) {
        const question = values["question"+q] || {
            count: 0,
            value: 0
        };

        
        question.value = newAverage(data[q].value, question.value, question.count)
        question.count++;

        values["question"+q] = question;
    }
    await refernceSaveData.set(values);

    const date = new Date();
    const today = dateToDataFormat(`${date.getDate()}.${date.getMonth() + 1}.${date.getFullYear()}`);

    const reference = admin.database().ref("calculator/"+userId+"/dynamic/"+today+"/type"+calcType);
    for (q = 0; q < data.length; ++q) {
        const updateData = {};
        updateData["value" + q] = data[q].formulaValue;
        await reference.update(updateData);
    }
    // // post to 0 - length



    res.sendStatus(200);


    
});

module.exports = router;