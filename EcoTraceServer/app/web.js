const parseData = require("./webParse")

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

async function getImgFromUrl(url, d) {
    try {
        const { data } = await axios.get(url);
        const $ = cheerio.load(data);
        let imgElement = $(d.classIn).first()
        if (d.widgetValue) {
            return imgElement.text().trim();
        } else {
            return imgElement.attr(d.widgetAttr);
        }
    } catch (error) {
        // console.error(`Failed to fetch image from ${url}:`, error);
        return null;
    }
}

async function fetchAndStoreNews() {
    try {
        const newsItems = [];
        for (const [lnk, data] of Object.entries(parseData)) {
            const response = await axios.get(lnk);
            const $ = cheerio.load(response.data);
 
            $(data.news).each(async (i, elem) => {
                let titles = [];
                let urls = [];
                let imgs = [];
    
                $(elem).find(data.newsTitle.classIn).each((j, elem1) => {
                    let titleElement = $(elem1);
                    if (data.newsTitle.widget) {
                        titleElement = titleElement.find(data.newsTitle.widget);
                    }
                        if (data.newsTitle.widgetValue) {
                            titles.push(titleElement.text().trim())
                        } else {
                            titles.push(titleElement.attr(data.newsTitle.widgetAttr))
                        }
                    
                });
    
                $(elem).find(data.newsLink.classIn).each((j, elem1) => {
                    let linkElement = $(elem1);
                    if (data.newsLink.widget) {
                        linkElement = linkElement.find(data.newsLink.widget);
                    }
                        if (data.newsLink.widgetValue) {
                            urls.push(linkElement.text().trim());
                        } else {
                            urls.push(linkElement.attr(data.newsLink.widgetAttr));
                        }
                    
                });
    
                if (!data.newsImage.gotoUrl) {
                    $(elem).find(data.newsImage.classIn).each((j, elem1) => {
                        let imgElement = $(elem1);
                        if (data.newsImage.widget) {
                            imgElement = imgElement.find(data.newsImage.widget);
                        }
                            if (data.newsImage.widgetValue) {
                                imgs.push(imgElement.text().trim());
                            } else {
                                imgs.push(imgElement.attr(data.newsImage.widgetAttr));
                            }
                        
                    });
                } else {
                    imgs.push(null)
                }
    
                for (var n = 0; n < titles.length; ++n) {
                    newsItems.push({
                        title: titles[n] || "",
                        url: urls[n] || "",
                        image: imgs[n] ? imgs[n] : await getImgFromUrl(urls[n] || null, data.newsImage),
                        source: data.source,
                        isRu: data.isRu && 1 || 0
                    })
                }
            });
        }

        console.log('News fetched and stored successfully');
        return newsItems;
    } catch (error) {
        console.error('Error fetching and storing news:', error);
    }
}


cron.schedule('0 0,2,4,6,8,10,12,14,16,18,20,22 * * *', async () => {
    const datas = await fetchAndStoreNews();

    await connection2.execute('delete from posts');

    for (const result of datas) {
        await connection2.execute(
            `insert into posts (source, postLink, postImage, postTitle, fetchTime, isRu) values(?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE 
    postLink = VALUES(postLink),
    postImage = VALUES(postImage),
    postTitle = VALUES(postTitle),
    fetchTime = VALUES(fetchTime);`,
            [result.source, result.url, result.image, result.title, new Date(), result.isRu]
        );
    }

    console.log("News fetched!")
});

router.get('/web', async (req, res) => {
    const now = new Date();
    now.setHours(now.getHours() % 2 == 0 ? now.getHours() + 2 : now.getHours() + 1,0,0,0)

    res.send(`Новый фетч случится ${now}`);
});

module.exports = router
