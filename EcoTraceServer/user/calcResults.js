const express = require('express');
const { checkOAuth2 } = require('../tech/oauth');
const {
    ChartJSNodeCanvas
} = require('chartjs-node-canvas');

const width = 640;
const height = 360;
const chartJSNodeCanvas = new ChartJSNodeCanvas({
    width,
    height
});

const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["calculator"]

const router = express.Router();
const calculators = ["Пища", "Вода", "Отходы", "Энергия", "Транспорт"]
const images = {
    0 : ["Виды мяса", "Овощи и фрукты", "Переработанные продукты", "Местные и сезонные"]
}

const specifies = {
    "00": {
        "0": {
            "name": "Говядина",
            "color": "255, 87, 51"
        },
        "1": {
            "name": "Свинина",
            "color": "51, 255, 87"
        },
        "2": {
            "name": "Курица",
            "color": "51, 87, 255"
        },
        "3": {
            "name": "Баранина",
            "color": "255, 51, 153"
        },
        "4": {
            "name": "Индейка",
            "color": "255, 204, 0"
        },
        "5": {
            "name": "Утка",
            "color": "210, 247, 166"
        },
        "6": {
            "name": "Кролик",
            "color": "199, 0, 57"
        },
        "7": {
            "name": "Рыба",
            "color": "144, 12, 63"
        },
        "8": {
            "name": "Морепродукты",
            "color": "88, 24, 69"
        }
    },
    "01": {
        "0": {
          "name":"Овощи",
          "color": "88, 24, 69"
        },
        "1": {
          "name": "Фрукты",
          "color": "199, 0, 57"
        }
    },
    "02": {
        "0": {
          "name": "Готовые продукты",
          "color": "255, 51, 153"
        },
        "1": {
          "name": "Закуски, к/и",
          "color": "144, 12, 63"
        },
        "2": {
          "name": "С добавками",
          "color": "51, 87, 255"
        }
    }
}



const advices = {
    0: ""
}

router.get('/calc/getNumImages', async (req, res) => {
    const calcType = req.query.cType || 0;

    res.json(calcType == 0 && 3 || 0)//images[calcType].length || 0)
});

router.get('/calc/getImage', async (req, res) => {
    const calcType = req.query.cType || 0;
    const imageId = req.query.img || 0;
    const userId = req.query.cid || '0';

    const inMonth = new Date();
    inMonth.setMonth(inMonth.getMonth() - 1);
    const startAt = inMonth.getTime();

    const [data] = await connection2.execute('select date, `data` from `dataspecify` where userId = ? and date >= ? and calculator = ?', [userId, startAt, calcType]);

    console.log(data)
    
    const names = Object.values(specifies[`${calcType}${imageId}`]);
    
    const uData = data.map(item => {
        const values = item.data.split('$')[calcType].split(';');
        return values;
    });

    const usingData = new Array(names.length * uData.length).fill(null);
    uData.forEach((entry, index) => {
        entry.forEach(val => {
            const fastData = val.split('_');
            usingData[index * names.length + parseInt(fastData[0]) - 1] = parseInt(fastData[1]);
        });
    });

    const uDates = data.map(item => {
        const date = new Date(item.date);
        return `${date.getDate()}.${date.getMonth() + 1}.${date.getFullYear()}`;
    });

    const usingDates = [];
    for (let i = 0; i < names.length * data.length; i++) {
        usingDates.push(uDates[Math.floor(i / names.length)]);
    }

    const labels = [];
    for (let i = 0; i < usingData.length; i++) {
        labels.push(names[i % names.length]);
    }

    const dataFull = [];
    usingData.forEach((val, index) => {
        let someData = names[index % names.length];
        let ind = index - 9 * Math.floor(index / names.length);

        if (!dataFull[ind]) {
            dataFull[ind] = {
                data: [val],
                backgroundColor: `rgba(${someData.color}, 0.2)`,
                borderColor: `rgba(${someData.color}, 1)`,
                borderWidth: 1,
                label: someData.name
            };
        } else {
            dataFull[ind].data.push(val);
        }
    });


    const configuration = {
        type: 'bar',
        data: {
            labels: uDates,
            datasets: dataFull
        },
        options: {
            plugins: {
                title: {
                    display: true,
                    text: images[calcType][imageId],
                    font: {
                        size: 30
                    }
                },
                customCanvasBackgroundColor: {
                    color: '#F8E9DD',
                }
            },
            layout: {
                padding: {
                    left: 20,
                    right: 20,
                    top: 20,
                    bottom: 20
                }
            },
            scales: {
                y: {
                    title: {
                        display: true,
                        text: "Граммы",
                        font: {
                            size: 20
                        }
                    }
                }
            }
        },
        plugins: [{
            id: 'customCanvasBackgroundColor',
            beforeDraw: (chart, args, options) => {
                const { ctx } = chart;
                ctx.save();
                ctx.globalCompositeOperation = 'destination-over';
                ctx.fillStyle = options.color || '#99ffff';
                ctx.fillRect(0, 0, chart.width, chart.height);
                ctx.restore();
            }
        }]
    };

    const image = await chartJSNodeCanvas.renderToBuffer(configuration);

    res.set('Content-Type', 'image/png');
    res.send(image);
});



router.get('/calc/getAdvices', async (req, res) => {
    const calcType = req.query.cType || 0;

    res.send(images[calcType] || 0)

});

module.exports = router;