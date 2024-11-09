const fs = require('fs');
const path = require('path');
const express = require('express');

const router = express.Router();

const VIEWS_DIR = path.join(__dirname, 'views');
const homeTemplate = fs.readFileSync(path.join(VIEWS_DIR, 'home.html'), 'utf8');

function render(template, data) {
    return template.replace(/\{\{(\w+)\}\}/g, (_, key) => String(data[key] ?? ''));
}

// GET /ping -- healthcheck, отвечает {status:'ok'}.
router.get('/ping', (req, res) => {
    res.status(200).json({ status: 'ok' });
});

// GET /assets/style.css -- статика для домашней страницы.
router.get('/assets/style.css', (req, res) => {
    res.type('css').sendFile(path.join(VIEWS_DIR, 'style.css'));
});

// GET / -- домашняя HTML-страница сервера.
router.get('/', (req, res) => {
    res.type('html').send(render(homeTemplate, {}));
});

module.exports = router;
