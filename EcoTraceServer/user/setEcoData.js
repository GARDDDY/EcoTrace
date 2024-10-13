const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["calculator"]

const router = express.Router();


function newAverage(plus, previous, count, update) {
    return (previous + plus) / (count + !update);
}

function getHourWord(hour) {
    if (hour % 10 === 1 && hour % 100 !== 11) {
        return "час";
    } else if ((hour % 10 >= 2 && hour % 10 <= 4) && (hour % 100 < 10 || hour % 100 >= 20)) {
        return "часа";
    } else {
        return "часов";
    } 
}

router.post('/setEcoData', async (req, res) => {
    const data = req.body;
    const calcType = req.query.cType || 0;
    const userId = req.query.cuid || '0';
    const oAuth = req.query.oauth || '0';


    console.log(data)

    if (!await checkOAuth2(oAuth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }

    const insertation = new Date().setHours(0,0,0,0);

    const [lastGot] = await connection1.execute(`select lastApplied${calcType} from calcexp where userId = ?`, [userId])
    const lastUpdate = lastGot[0][`lastApplied${calcType}`]
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);

    const daysSinceLast = Math.floor(new Date() - new Date(lastUpdate) / (1000 * 60 * 60 * 24));

    console.warn(lastGot)
    
    const isUpdating = lastUpdate >= today.getTime() && lastUpdate < tomorrow.getTime();


    const [columns] = await connection2.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_NAME = 'userstatics' AND COLUMN_NAME LIKE ?`, [`%${calcType}%`]);

    const columnNames = columns.map(col => col.COLUMN_NAME).join(', ');

    if (columnNames.length === 0) {
        return res.send(null);
    }

    const query = `SELECT ${columnNames} FROM userstatics WHERE userId = ?`;
    const [previous] = await connection2.execute(query, [userId]);


    var fullData = ""
    var specifyData = {};
    for (let i = 0; i < data.length; i++) {
        specifyData[i] = '';
    }
    var specified = false;
    for (const element of data) {
        const questionId = element.question;
        const prevData = previous.length > 0 ? previous[0][`q${calcType}${questionId}`].split(";") : ["0", "0"];
        if (element.useSpecify) {
            const specifyValues = Object.values(element.specify);
            const sumSpecify = specifyValues.reduce((acc, val) => acc + val, 0);
            const timesAverage = specifyValues.length;
        
            const average = Math.round(newAverage(
                sumSpecify, 
                parseFloat(prevData[0]), 
                parseInt(prevData[1]),
                isUpdating
            ));

            await connection2.execute(`UPDATE userstatics SET q${calcType}${questionId} = ? WHERE userId = ?`, 
                [`${average};${parseInt(prevData[1]) + !isUpdating}`, userId]);

            specifyData[questionId] = Object.entries(element.specify)
                .map(([key, value]) => `${key}_${value}`)
                .join(';');
            specified = true;

            fullData += `${sumSpecify};`

        } else {
            const average = Math.round(newAverage(
                element.value, 
                parseFloat(prevData[0]), 
                parseInt(prevData[1]),
                isUpdating
            ));
        
            await connection2.execute(`UPDATE userstatics SET q${calcType}${questionId} = ? WHERE userId = ?`, 
                [`${average};${parseInt(prevData[1]) + !isUpdating}`, userId]);

            fullData += `${element.value};`
        }
    }

    fullData = fullData.slice(0, -1)

    if (isUpdating) {
        const [existingData] = await connection2.execute(`
            SELECT data FROM \`data\` WHERE userId = ? AND date = ?`, 
            [userId, insertation]);
        let currentData = existingData[0].data.split(';');

        const newData = fullData.split(';');
        const updatedData = currentData.map((value, index) => {
            return parseInt(value) + parseInt(newData[index] || 0);
        }).join(';');
        
        await connection2.execute(`
            UPDATE \`data\` SET data = ? WHERE userId = ? AND date = ?`, 
            [updatedData, userId, insertation]);

    } else {
        await connection2.execute(`
            INSERT INTO \`data\` (userId, date, calculator, data) 
            VALUES (?, ?, ?, ?)`, 
            [userId, insertation, calcType, fullData]);
    }




    if (specified) {
        const str = Object.values(specifyData).join("$");
        if (!isUpdating) {
            await connection2.execute(`
                INSERT INTO \`dataspecify\` (userId, calculator, date, data) 
                VALUES (?, ?, ?, ?)`, 
                [userId, calcType, insertation, str]);
        } else {
            const [prev] = await connection2.execute(`
                select data from \`dataspecify\` where userId = ? and calculator = ? and date = ?`, 
                [userId, calcType, insertation]);

            const previous = prev[0].data.split('$')
            
            for (const [index, value] of Object.entries(specifyData)) {
                const pairs = value.split(';');
                for (const pair of pairs) {
                    const [key, increment] = pair.split('_');
                    const newValue = parseInt(increment);
            
                    const currentRow = previous[index];
                    if (currentRow) {
                        const values = currentRow.split(';');
                        let found = false;
            
                        for (let j = 0; j < values.length; j++) {
                            const [currentKey, currentValue] = values[j].split('_');
                            if (currentKey === key) {
                                values[j] = `${currentKey}_${parseInt(currentValue) + newValue}`;
                                found = true;
                                break;
                            }
                        }
            
                        if (!found) {
                            values.push(`${key}_${newValue}`);
                        }
            
                        previous[index] = values.join(';');
                    } else {
                        previous[index] = `${key}_${newValue}`;
                    }
                }
            }

            const newVal = previous.join('$');

            console.log(newVal)

            await connection2.execute(`
                Update \`dataspecify\` set data = ? where userId = ? and calculator = ? and date = ?`, 
                [newVal, userId, calcType, insertation]);
        }
    }



    
    if (isUpdating) {
        console.log(`got ${lastUpdate} and its less then ${tomorrow.getTime()} and more then ${today.getTime()}`)
        const difference = tomorrow.getTime() - lastUpdate
        const hours = Math.ceil(difference / (1000 * 60 * 60));
        res.json({message : "Вы обновили свои данные! Менее чем через "+hours+' '+getHourWord(hours)+" их уже нельзя будет обновить!"})
    } else {
        console.log(`got ${lastUpdate} and its more then ${tomorrow.getTime()} or less then ${today.getTime()}`)
        await connection1.execute(`UPDATE calcexp SET lastApplied${calcType} = ? WHERE userId = ?`, [insertation, userId]);
        const [experience] = await connection1.execute(`select experience from user where userId = ?`, [userId])
        console.log(experience[0])
        await connection1.execute(`UPDATE user SET experience = experience + ? WHERE userId = ?`, [Math.floor((experience[0].experience + 100) / 100), userId]);
        res.json({message : "Вы добавили свои данные и полу+чили баллы!"})
    }

    res.status(200)

});

module.exports = router;