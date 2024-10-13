const express = require('express');

const connections = require("../server")
const connection2 = connections["users"]

const router = express.Router();

router.get('/applyEdu', async (req, res) => {
    const userId = req.query.cid || '0';
    const oAuth = req.query.oauth || '0';
    const edu = req.query.edu || -1;

    if (edu == -1){
        return res.send([false]);
    }

    const [status] = await connection2.execute(`select edu${edu} from education where userId = ?`, [userId])
    console.log(status)
    const key = Object.keys(status[0])[0];

    console.log(key)
    const value = status[0][key];

    console.log(value)
    const booleanStatus = Boolean(value);

    console.log(booleanStatus)

    if (!booleanStatus) {

    await connection2.execute(`UPDATE education SET edu${edu} = ? WHERE userId = ?`, [true, userId])
    await connection2.execute(`UPDATE user SET experience = experience + 5 WHERE userId = ?`, [userId])

    return res.send([true])
    } else return res.send([false])



});

module.exports = router