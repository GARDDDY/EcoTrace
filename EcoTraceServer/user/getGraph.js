const {
    ChartJSNodeCanvas
} = require('chartjs-node-canvas');
const express = require('express');
// to sql

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

async function getDynamicDataByDate(userId, date, dataType) {

    try {
        const reference = admin.database().ref('calculator/' + userId + '/dynamic/' + dateToDataFormat(date) + "/type" + dataType);
        const snapshot = await reference.once('value');
        const value = snapshot.val() || {};

        var sum = Object.values(value).reduce((total, current) => total + current, null);

        return sum;
    } catch (error) {
        return null;
    }

}

router.get('/getUserGraph', async (req, res) => {
    const userId = req.query.uid || '0';
    const timeSort = parseInt(req.query.time, 10) || 0;
    const typesSortReq = req.query.types || null;
    let typesSort = typesSortReq == null ? null : typesSortReq.split(',').map(Number)

    const today = new Date();
    let times = [];

    // dates
    switch (timeSort) {
        case 0: {
            for (let day = 7; day >= 0; --day) {
                let date = new Date(today);
                date.setDate(today.getDate() - day);
                times.push(`${date.getDate()}.${date.getMonth() + 1}.${date.getFullYear()}`);
            }
            break;
        }
        case 1: {
            for (let day = 30; day >= 0; day -= 3) {
                let date = new Date(today);
                date.setDate(today.getDate() - day);
                times.push(`${date.getDate()}.${date.getMonth() + 1}.${date.getFullYear()}`);
            }
            break;
        }
        default:
            console.log('Unknown timeSort value');
            break;
    }

    // datasets
    const dataColors = [
        ['rgba(85,255,85,0.1)', 'rgba(85,255,85,1.0)'],
        ['rgba(85,85,255,0.1)', 'rgba(85,85,255,1.0)'],
        ['rgba(0,0,0,0.1)', 'rgba(0,0,0,1.0)'],
        ['rgba(154, 205, 50,0.1)', 'rgba(154, 205, 50,1.0)'],
        ['rgba(188, 143, 143,0.1)', 'rgba(188, 143, 143,1.0)']
    ]
    const labels = ["Пища", "Вода", "Отходы", "Энергия", "Транспорт"]

    var dataset = [];
    for (dataType = 0; dataType < 5; ++dataType) {
        if (typesSort == null || !typesSort.includes(dataType)) {

            const valuesPromises = times.map(time => getDynamicDataByDate(userId, time, dataType));

            try {
                const values = await Promise.all(valuesPromises);

                dataset.push({
                    label: labels[dataType % 5],
                    backgroundColor: dataColors[dataType % 5][0],
                    borderColor: dataColors[dataType % 5][1],
                    fill: true,
                    data: values
                });
            } catch (error) {
                console.error('Error processing dataType', dataType, ':', error);
            }
        }
    }


    try {
        const configuration = {
            type: 'line',
            data: {
                labels: times,
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

        // Генерируем изображение графика
        const image = await chartJSNodeCanvas.renderToBuffer(configuration);

        // Устанавливаем заголовок и возвращаем изображение
        res.set('Content-Type', 'image/png');
        res.send(image);
    } catch (error) {
        console.error('Error generating chart:', error);
        res.status(500).send('Internal Server Error');
    }
});

module.exports = router;
