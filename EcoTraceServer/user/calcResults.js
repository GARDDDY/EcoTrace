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
    },
    "35": {
        "0": {
            "name": "ЭП",
            "color": "255, 0, 0"
        },
        "1": {
            "name": "ЭП",
            "color": "255, 0, 0"
        },
        "2": {
            "name": "ЭП",
            "color": "255, 0, 0"
        },
        "3": {
            "name": "ЭП",
            "color": "255, 0, 0"
        },
        "4": {
            "name": "ЭП",
            "color": "255, 0, 0"
        }
    },
    "44": {
        "0": {
            "name": "ЭП",
            "color": "255, 0, 0"
        },
        "1": {
            "name": "ЭП",
            "color": "255, 0, 0"
        },
        "2": {
            "name": "ЭП",
            "color": "255, 0, 0"
        },
        "3": {
            "name": "ЭП",
            "color": "255, 0, 0"
        },
        "4": {
            "name": "ЭП",
            "color": "255, 0, 0"
        }
    }
}



const advices = {
    0: ""
}

const imagesAvailableFor = [
    "0_1",
    "0_2",
    "0_3",
]

router.get('/calc/getNumImages', async (req, res) => {
    res.send([imagesAvailableFor.join()])
});


function sDay(day) {
    const lastDigit = day % 10

    if (Math.floor(day / 10) % 10 === 1) {
        return "дней"
    }

    switch (lastDigit) {
        case 1:
            return "день";
        case 2:
        case 3:
        case 4:
            return "дня";
        default:
            return "дней";
    }
}

function toStringPeriod(per) {
    if (per === 0) {
        return "Вчера"
    } else if (per === 1) {
        return "Позавчера"
    } else {
        return `${per+1} ${sDay(per+1)} назад`
    }
}

function daysSince(timestamp) {
    const now = Date.now();
    const difference = now - timestamp; 
    const days = Math.floor(difference / (1000 * 60 * 60 * 24));
    return days;
}

router.get('/calc/getImage', async (req, res) => {
    const calcType = req.query.cType || 0;
    const imageId = req.query.img || 0;
    const userId = req.query.cid || '0';

    try {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    yesterday.setHours(0,0,0,0)

    const weekAgo = new Date(yesterday);
    weekAgo.setDate(yesterday.getDate() - 7);
    weekAgo.setHours(0,0,0,0)

    console.log(weekAgo.getTime(), yesterday.getTime())


    const [data] = await connection2.execute('select date, `data` from `dataspecify` where userId = ? and date >= ? and date <= ? and calculator = ?', 
        [userId, weekAgo.getTime(), yesterday.getTime(), calcType]);


    const graphData = []
    const labelsAll = []

    data.forEach(element => {
        const values = element.data.split("$")
        const date = element.date;

        const needed = values[imageId].split(";")

        const groupData = []
        needed.forEach(element =>{
            const spec = element.split("_")

            const specName = specifies[`${calcType}${imageId}`][parseInt(spec[0])-1]
            const specValue = spec[1]

            groupData.push({lD: specName, v: specValue})
            labelsAll.push(toStringPeriod(daysSince(date)))
        })

        graphData.push(groupData)
        
    });

    console.log(graphData)

    res.status(200)

    const labels = [...new Set(labelsAll)].reverse();
    console.log(labels)

    const values = {};
    graphData.forEach(data =>{
        data.forEach(e => {
            if (values[e.lD.name] == null) {
                values[e.lD.name] = {
                    label: e.lD.name,
                    backgroundColor: `rgba(${e.lD.color}, 0.6)`,
                    borderColor: `rgba(${e.lD.color}, 1)`,

                    data: [e.v]
                }
            } else {
                values[e.lD.name].data.push(e.v)
            }
        })
    })

    console.log(values)
    



    const configuration = {
        type: 'bar',
        data: {
            labels: labels,
            datasets: Object.values(values)
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
                },
                x : {
                    autoSkip: true,
                    maxTicksLimit: 10
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
} catch (e) {
    res.set('Content-Type', 'image/png');
    res.send(null);
}

    
});



function getGenderString(g) {
    return g == 0 && "мужчин" || "женщин"
}

router.get('/calc/getAdvices', async (req, res) => {
    const calcType = req.query.cType || 0;
    const image = req.query.img || 1;
    const userId = req.query.cid || '0';

    const [userGender] = await connection1.execute("select gender from user where userId = ?", [userId])
    if (userGender.length === 0) {
        return res.send([])
    }
    const [sameUserGenders] = await connection1.execute('select userId from user where gender = ?', [userGender[0].gender || 0])

    const usersIds = sameUserGenders.map(user => user.userId);
    const placeholders = usersIds.map(a => `"${a}"`).join(', ');

    const [averageForThisGender] = await connection2.execute(`SELECT calculator, userId, SUBSTRING_INDEX(SUBSTRING_INDEX(data, '$', ?), '$', -1) as data, date FROM dataspecify WHERE userId IN (${placeholders}) and calculator = ?`, [image, calcType]);

const values = [];

averageForThisGender.forEach(entry => {
    const numbers = entry.data.split(';').map(item => parseInt(item.split('_')[1]));
    values.push(...numbers);
});
const totalSum = values.reduce((acc, value) => acc + value, 0);

const averageByRows = values.length > 0 ? totalSum / values.length : 0;
const uniqueUsers = new Set(averageForThisGender.map(entry => entry.userId)).size;
const averageByUsers = uniqueUsers > 0 ? totalSum / uniqueUsers : 0;

res.send([`Среднее значение для всех ${getGenderString(userGender[0].gender)} на сервисе = ${averageByRows} гр.`, 
        `Среднее значение для всех уникальных ${getGenderString(userGender[0].gender)} на сервисе = ${averageByUsers} гр.`]);
});

module.exports = router;