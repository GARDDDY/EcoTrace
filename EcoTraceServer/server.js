require('dotenv').config();
const http = require('http');
const logger = require('./logger');
const { ping } = require('./core/db');
const { migrate } = require('./core/migrate');
const app = require('./app');
const { init: initRealtime } = require('./core/realtime');

const port = 35288;
const server = http.createServer(app);
initRealtime(server);

process.on('uncaughtException', (err) => {
    logger.error('Uncaught exception', { err: err.message, stack: err.stack });
});
process.on('unhandledRejection', (reason) => {
    logger.error('Unhandled rejection', { reason: String(reason) });
});

ping()
    .then(() => migrate())
    .then(() => server.listen(port, () => logger.info(`Server is running at http://localhost:${port}`)))
    .catch((err) => logger.error('Error starting server', { err: err.message, stack: err.stack }));
