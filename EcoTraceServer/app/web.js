// const parseData = require("webParse")

const axios = require('axios');
const cheerio = require('cheerio');
const schedule = require('node-schedule');

const express = require('express');

const connections = require("../server")
const connection2 = connections["web"]

const router = express.Router();

function extractBase64(style) {
    const base64Match = style ? style.match(/url\(["']data:image\/[^"']*base64,([^"']*)["']\)/) : null;
    return base64Match ? base64Match[1] : null;
}

// Функция для сохранения base64 строки как изображения
async function fetchImageFromUrl(url) {
    try {
        const { data } = await axios.get(url);
        const $ = cheerio.load(data);
        const img = $('img[data-testid="prism-image"]').first().attr('src');
        return img ? new URL(img, url).href : null;
    } catch (error) {
        console.error(`Failed to fetch image from ${url}:`, error);
        return null;
    }
}

async function fetchAndStoreNews() {
    try {
        const { data } = await axios.get('https://www.nationalgeographic.com/environment');
        const $ = cheerio.load(data);
        // res.json($.html());

        const newsItems = [];
        $('.col.col-bottom-gutter').each((i, elem) => {
            const title = $(elem).attr('aria-label').trim();
            const url = $(elem).find('.AnchorLink.PromoTile__Link').attr('href');
            if (title && url) {
                newsItems.push({
                    title: title,
                    url: url.startsWith('http') ? url : `https://www.nationalgeographic.com${url}`
                });
            }
        });

       

        console.log('News fetched and stored successfully');
        return newsItems
    } catch (error) {
        console.error('Error fetching and storing news:', error);
    }
}


router.get('/web', async (req, res) => {
    const datas = await fetchAndStoreNews()
    const promises = datas.map(async (item) => {
        item.image = await fetchImageFromUrl(item.url);
        return item;
    })

    const results = await Promise.all(promises);

    for (const result of results) {
        await connection2.execute(
            `insert into posts (source, postLink, postImage, postTitle, fetchTime) values(?,?,?,?,?)
            ON DUPLICATE KEY UPDATE 
    postLink = VALUES(postLink),
    postImage = VALUES(postImage),
    postTitle = VALUES(postTitle),
    fetchTime = VALUES(fetchTime);`,
            ["NG", result.url, result.image, result.title, new Date()]
        )
    }

    res.send(results);
});

module.exports = router

// Периодический запуск задачи
// schedule.scheduleJob('0 * * * *', fetchAndStoreNews); // Каждый час
