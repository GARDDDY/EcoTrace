const express = require('express');
require('express-async-errors');
const logger = require('./logger');
const { errorHandler } = require('./core/route');
const { localPath } = require('./storage');

const app = express();
app.use(express.json());

app.use(require('./pages/home'));
app.use(require('./pages/constants'));

app.use(require('./auth/routes'));

app.use(require('./users/profile'));
app.use(require('./users/friends'));
app.use(require('./users/membership'));
app.use(require('./users/updates'));
app.use(require('./users/education'));

app.use(require('./groups/group'));
app.use(require('./groups/members'));
app.use(require('./groups/posts'));
app.use(require('./groups/comments'));

app.use(require('./events/event'));
app.use(require('./events/members'));

app.use(require('./calculator/calculators'));
app.use(require('./calculator/charts'));

app.use(require('./news/news'));
app.use(require('./rating/rating'));

app.get('/uploads/:bucket/:filename', (req, res) => {
    const { bucket, filename } = req.params;
    if (/[\\/]/.test(bucket) || /[\\/]/.test(filename)) return res.status(400).end();
    res.sendFile(localPath(bucket, filename + '.png'), (err) => {
        if (err) res.status(404).end();
    });
});

app.use(errorHandler(logger));

module.exports = app;
