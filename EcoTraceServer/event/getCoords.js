const express = require('express');

const connections = require("../server")
const connection2 = connections["events"]

const router = express.Router();

router.get('/getEventCoords', async (req, res) => {
    const eventId = req.query.eventId || '0';

    if (!connection2) {
        console.error("not connected to events")
        return
    }

    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    const data = []

    const [coords] = await connection2.execute(
        `SELECT * FROM eventcoords WHERE eventId = "${eventId}"`
    );
    coords.forEach(element => {
        const obj = {
            objectName: element.coordText,
            objectType: element.coordType,
            objectRelation: element.relationWithTime,
            objectCenter: {
                latitude: element.latitude,
                longitude: element.longitude
            },
            circleRadius: element.coordRadius,
            fillColor: element.coordColor1,
            strokeColor: element.coordColor2
        }
        data.push(obj)
    });

    res.json(data);
});

module.exports = router;
