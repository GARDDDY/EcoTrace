const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["calculator"]

const router = express.Router();


function newAverage(plus, previous, count, update) {
    return parseInt((previous + plus) / (count + !update));
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

async function isUpdatingF(calcType, userId) {
    const [lastGot] = await connection1.execute(`select lastApplied${calcType} from calcexp where userId = ?`, [userId])
    const lastUpdate = lastGot[0][`lastApplied${calcType}`]
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);

    const r = lastUpdate >= today.getTime() && lastUpdate < tomorrow.getTime();

    console.log("upd: ", r)
    return r
}

const multer = require('multer');
const path = require('path');
const { all } = require('axios');
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, './EcoTraceServer/groups/uploads');
    },
    filename: function (req, file, cb) {
        cb(null, Date.now() + path.extname(file.originalname));
    }
});

const upload = multer({ storage: storage });

const ks = [
    2.5/1000,
    0.15 / 1000,
    1 / 1000,
    0.5,
    0.25
]

router.post('/setEcoData', upload.single('image'), async (req, res) => { // check todo
    const data = JSON.parse(req.body.jsonData);
    const calcType = req.query.cType || 0;
    const userId = req.query.cuid || '0';
    const oAuth = req.query.oauth || '0';

    console.log("Data", data)

    

    if (!await checkOAuth2(oAuth, userId)) {
        return res.status(403).json({ error: "You are not signed in! Not allowed" });
    }



    
    const insertation = new Date().setHours(0,0,0,0);
    
    const isUpdating = await isUpdatingF(calcType, userId)


    const [columns] = await connection2.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_NAME = 'userstatics' AND COLUMN_NAME LIKE ?`, [`q${calcType}%`]);

    const columnNames = columns.map(col => col.COLUMN_NAME).join(', ');

    if (columnNames.length === 0) {
        return res.send(null);
    }

    const allNew = []
    const [previousRowsMap] = await connection2.execute(`SELECT ${columnNames} FROM userstatics WHERE userId = ?`, [userId]);
    const previousRows = Object.values(previousRowsMap[0])
    console.log(previousRows)
    const updatedRows = data.map((item, i) => {
        const [previousAvg, previousCount] = previousRows[i] ? previousRows[i].split(';') : [0, 0];
        const dataSumValue = item.useSpecify
            ? Object.values(item.specify).reduce((acc, curr) => acc + curr, 0)
            : item.value;
        allNew.push(dataSumValue)
        const newAvg = newAverage(dataSumValue, parseInt(previousAvg), parseInt(previousCount), isUpdating);
        const newCount = parseInt(previousCount) + 1;
        return `${newAvg};${newCount}`;
    });

    const updatePromises = updatedRows.map((updatedValue, i) => {
        const columnName = columnNames.split(',')[i];
        return connection2.execute(
            `UPDATE userstatics SET ${columnName} = ? WHERE userId = ?`,
            [updatedValue, userId]
        );
    });

    await Promise.all(updatePromises);
    console.log("avergaes updated")


    if (isUpdating) {
        const [previousRows] = await connection2.execute(
            'SELECT `data` FROM `data` WHERE userId = ? AND calculator = ? AND date = ?', 
            [userId, calcType, insertation]
        );
        var previous = previousRows[0].data.split(';');
        for (var i = 0; i < allNew.length; ++i) {
            previous[i] = ((parseInt(previous[i]) + allNew[i]) * ks[calcType]).toString();
        }
        const updatedData = previous.join(';');
        await connection2.execute(
            'UPDATE `data` SET `data` = ? WHERE userId = ? AND calculator = ? AND date = ?', 
            [updatedData, userId, calcType, insertation]
        );

        console.log("data updated")
    } else {
        await connection2.execute(
            'insert into `data` (userId, date, calculator, `data`) values (?,?,?,?)', 
            [userId, insertation, calcType, allNew.join(';')]
        );

        console.log("data inserted")
    }


    if (isUpdating) {
        try {
            const [previousRows] = await connection2.execute(
                'SELECT `data` FROM `dataspecify` WHERE userId = ? AND calculator = ? AND date = ?', 
                [userId, calcType, insertation]
            );

            if (previousRows.length == 0) {
                
            }
            const previous = previousRows[0].data.split('$')
            const newDatas = []
            
            for (var i = 0; i < data.length; ++i) {
                if (!data[i].useSpecify) {
                    newDatas.push("")
                    continue;
                }
                const prevImage = previous[i].split(';')
                const hashMap = {};
                prevImage.forEach(pair => {
                    const [key, value] = pair.split('_');
                    hashMap[parseInt(key)] = parseInt(value);
                });

                Object.entries(data[i].specify).forEach(([i, v]) => {
                    hashMap[parseInt(i)] = hashMap[parseInt(i)] ? hashMap[parseInt(i)] + v : v
                })

                const newData = Object.entries(hashMap)
                    .map(([key, value]) => `${key}_${value}`)
                    .join(';');

                newDatas.push(newData)
            }

            await connection2.execute(
                'update `dataspecify` set `data` = ? WHERE userId = ? AND calculator = ? AND date = ?', 
                [newDatas.join('$'), userId, calcType, insertation]
            );
            
            console.log("data updated")
        } catch(e) {
            console.log("failed to update specifies")
        }
    } else {
        const newDatas = []
        for (var i = 0; i < data.length; ++i) {
            if (!data[i].useSpecify) {
                newDatas.push("")
                continue;
            }

            const newData = Object.entries(data[i].specify)
                .map(([key, value]) => `${key}_${value}`)
                .join(';');

            newDatas.push(newData)
        }

        await connection2.execute(
            'insert into `dataspecify` (userId, calculator, date, data) values(?,?,?,?) ', 
            [userId, calcType, insertation, newDatas.join('$')]
        );

        console.log("data inserted")
    }

    
    if (isUpdating) {
        // console.log(`got ${lastUpdate} and its less then ${tomorrow.getTime()} and more then ${today.getTime()}`)
        // const difference = tomorrow.getTime() - lastUpdate
        // const hours = Math.ceil(difference / (1000 * 60 * 60));
        
        res.json({message : "Вы обновили свои данные!"})// Менее чем через "+hours+' '+getHourWord(hours)+" их уже нельзя будет обновить!"})
    } else {
        // console.log(`got ${lastUpdate} and its more then ${tomorrow.getTime()} or less then ${today.getTime()}`)
        // await connection1.execute(`UPDATE calcexp SET lastApplied${calcType} = ? WHERE userId = ?`, [insertation, userId]);
        await connection1.execute(`update calcexp set lastApplied${calcType} = ? where userId = ?`, [insertation, userId])
        const [experience] = await connection1.execute(`select experience from user where userId = ?`, [userId])
        console.log(experience[0])
        const exp = Math.floor((experience[0].experience + 100) / 100)
        await connection1.execute(`UPDATE user SET experience = experience + ? WHERE userId = ?`, [exp, userId]);
        res.json({message : "Вы добавили свои данные и получили баллы: "+exp})
    }

    res.status(200)

});

module.exports = router;