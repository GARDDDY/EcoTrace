const {
    ChartJSNodeCanvas
} = require('chartjs-node-canvas');
const express = require('express');
const connections = require("../server")
const connection1 = connections["users"]
const connection2 = connections["calculator"]

const router = express.Router();

const width = 640;
const height = 360;
const chartJSNodeCanvas = new ChartJSNodeCanvas({
    width,
    height
});

function dateToDataFormat(date) {
    const dateList = date.split('.').map(String)
    return dateList[2]
        +dateList[1].padStart(2, "0")
        +dateList[0].padStart(2, "0");
} 


router.get('/getUserGraph', async (req, res) => {
    const userId = req.query.uid || '0';
    const timeSort = parseInt(req.query.time, 10) || 0;
    const typesSortReq = req.query.types || null;
    let typesSort = typesSortReq == null ? [] : typesSortReq.split(',').map(Number);

    const today = new Date();
    today.setHours(0,0,0,0)
    today.setDate()
    console.log(today.getTime())
    const times = [];
    const timesString = []

    switch (timeSort) {
        case 0: {
            for (var d = 0; d < 7; ++d) {
                const day = new Date();
                day.setHours(0,0,0,0)
                day.setDate(day.getDate()-d)
                times.push(day.getTime())
                timesString.push(`${day.getDate()}.${day.getMonth() + 1}.${day.getFullYear()}`);
            }
            break;
        }
        case 1: {
            for (var d = 0; d < 30; ++d) {
                const day = new Date();
                day.setHours(0,0,0,0)
                day.setDate(day.getDate()-d)
                times.push(day.getTime())
                timesString.push(`${day.getDate()}.${day.getMonth() + 1}.${day.getFullYear()}`);
            }
            break;
        }
        default:
            console.log('Unknown timeSort value');
            break;
    }

    const minDate = Math.min(...times);

    const dataColors = [
        ['85,255,85', '20,66,20'],
        ['85,85,255', '20,20,66'],
        ['0,0,0', '255,255,255'],
        ['154,205,50', '98,130,33'],
        ['188,143,143', '82,60,60']
    ]
    const labels = ["Пища", "Вода", "Отходы", "Энергия", "Транспорт"]

    const dataset = []
    for (var i = 0; i < 4; ++i) {
        if (!typesSort.includes(i)) {
            const [data] = await connection2.execute('select data, date from data where userId = ? and date >= ? and calculator = ?', [userId, minDate, i])
            const dateToData = {}
            for (let j = 0; j < data.length; ++j) {
                const utcDate = new Date(data[j].date).setHours(0, 0, 0, 0);
                dateToData[utcDate] = data[j].data;
            }
            
            console.log(dateToData);
            
            const sums = times.map((timestamp) => {
                if (dateToData[timestamp]) {
                    return dateToData[timestamp].split(';').map(num => parseInt(num, 10)).reduce((acc, curr) => acc + curr, 0);
                } else {
                    return null;
                }
            }).reverse();
            const autos = [null];
            for (var j = 1; j < sums.length-1; ++j) {
                if (!sums[j]) {
                    if (sums[j-1] && sums[j+1]) {
                        autos[j] = (sums[j-1]+sums[j+1]) / 2
                        autos[j-1] = sums[j-1]
                        autos[j+1] = sums[j+1]
                    } else {
                        autos[j] = null
                    }
                } else {
                    if (!autos[j]) {
                        autos[j] = null
                    }
                }
            }
            autos.push(null)
            if (!sums.every(e => e === null)) {
                dataset.push({
                    label: labels[i],
                    backgroudColor: `rgba(${dataColors[i][0]},1)`,
                    borderColor: `rgba(${dataColors[i][0]},1)`,
                    data: sums
                })
            }
            if (!autos.every(e => e === null)) {
                dataset.push({
                    label: `Автоматическое заполнение (${labels[i]})`,
                    backgroudColor: `rgba(${dataColors[i][1]},1)`,
                    borderColor: `rgba(${dataColors[i][1]},1)`,
                    data: autos
                })
            }
        }
    }


    try {
        const configuration = {
            type: 'line',
            data: {
                labels: timesString.reverse(),
                datasets: dataset
            },
            options: {
                plugins: {
                    legend: {
                        display: true
                    },
                    tooltip: {
                        callbacks: {
                            label: function (tooltipItem) {
                                return tooltipItem.dataset.label + ': ' + tooltipItem.raw;
                            }
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
                            text: 'Углеродные выбросы в тоннах',
                            font: {
                                size: 20
                            }
                        }
                    },
                    x: {
                        autoSkip: true,
                        maxTicksLimit: 10
                    }
                }
            },
            plugins: [{
                id: 'customCanvasBackgroundColor',
                beforeDraw: (chart, args, options) => {
                    const {
                        ctx
                    } = chart;
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
    } catch (error) {
        console.error('Error generating chart:', error);
        res.status(500).send('Internal Server Error');
    }
});

module.exports = router;
