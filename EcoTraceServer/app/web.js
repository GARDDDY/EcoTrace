// const parseData = require("webParse")

const axios = require('axios');
const cheerio = require('cheerio');
const cron = require('node-cron');

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


cron.schedule('0 0,2,4,6,8,10,12,14,16,18,20,22 * * *', async () => {
    const datas = await fetchAndStoreNews();
    const promises = datas.map(async (item) => {
        item.image = await fetchImageFromUrl(item.url);
        return item;
    });

    const results = await Promise.all(promises);

    await connection2.execute('delete from posts');

    for (const result of results) {
        await connection2.execute(
            `insert into posts (source, postLink, postImage, postTitle, fetchTime) values(?,?,?,?,?)
            ON DUPLICATE KEY UPDATE 
    postLink = VALUES(postLink),
    postImage = VALUES(postImage),
    postTitle = VALUES(postTitle),
    fetchTime = VALUES(fetchTime);`,
            ["National Geographic", result.url, result.image, result.title, new Date()]
        );
    }

    console.log("News fetched!")
});

router.get('/web', (req, res) => {
    const now = new Date();
    now.setHours(now.getHours() % 2 == 0 ? now.getHours() + 2 : now.getHours() + 1,0,0,0)
    res.send(`Новый фетч случится ${now}`);
});

module.exports = router
