const express = require('express');
const axios = require('axios');
const cheerio = require('cheerio');
const cron = require('node-cron');
const parseData = require('./webParse');
const { route } = require('../core/route');
const { query, pool } = require('../core/db');
const logger = require('../logger');

const router = express.Router();

async function imgFromArticle(url, cfg) {
    if (!url) return null;
    try {
        const { data } = await axios.get(url);
        const $ = cheerio.load(data);
        const el = $(cfg.classIn).first();
        return cfg.widgetValue ? el.text().trim() : el.attr(cfg.widgetAttr);
    } catch {
        return null;
    }
}

async function fetchNews() {
    const items = [];
    for (const [link, cfg] of Object.entries(parseData)) {
        let html;
        try {
            ({ data: html } = await axios.get(link));
        } catch (e) {
            logger.warn('News source unreachable', { link, error: e.message });
            continue;
        }
        const $ = cheerio.load(html);

        const blocks = $(cfg.news).toArray();
        for (const elem of blocks) {
            const pick = (sel) => {
                const out = [];
                $(elem).find(sel.classIn).each((_, e1) => {
                    let el = $(e1);
                    if (sel.widget) el = el.find(sel.widget);
                    out.push(sel.widgetValue ? el.text().trim() : el.attr(sel.widgetAttr));
                });
                return out;
            };

            const titles = pick(cfg.newsTitle);
            const urls = pick(cfg.newsLink);
            const imgs = cfg.newsImage.gotoUrl ? [] : pick(cfg.newsImage);

            for (let n = 0; n < titles.length; ++n) {
                items.push({
                    title: titles[n] || '',
                    url: urls[n] || '',
                    image: imgs[n] || await imgFromArticle(urls[n], cfg.newsImage),
                    source: cfg.source,
                    isRu: !!cfg.isRu,
                });
            }
        }
    }
    return items;
}

async function refreshNews() {
    const items = await fetchNews();
    const valid = items.filter((it) => it.url);
    if (!valid.length) return 0;

    await pool.query('DELETE FROM news_post');
    for (const it of valid) {
        await pool.query(
            `INSERT INTO news_post (source, url, image_url, title, is_russian)
             VALUES ($1, $2, $3, $4, $5) ON CONFLICT (url) DO NOTHING`,
            [it.source, it.url, it.image, it.title, it.isRu]
        );
    }
    logger.info('News cache refreshed', { count: valid.length });
    return valid.length;
}

if (process.env.NODE_ENV !== 'test') {
    cron.schedule('0 */2 * * *', () => {
        refreshNews().catch((e) => logger.error('News refresh failed', { error: e.message }));
    });
}

// GET /news -- лента новостей (публично).
router.get('/news', route(async () => {
    return query(
        `SELECT source, url, image_url, title, is_russian, fetched_at
         FROM news_post ORDER BY fetched_at DESC`
    );
}, { auth: false }));

// GET /news/status -- когда следующий фетч.
router.get('/news/status', route(async () => {
    const next = new Date();
    next.setMinutes(0, 0, 0);
    next.setHours(next.getHours() + (next.getHours() % 2 === 0 ? 2 : 1));
    return { nextFetch: next.toISOString() };
}, { auth: false }));

module.exports = router;
