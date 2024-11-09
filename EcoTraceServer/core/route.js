const { authenticate } = require('./auth');

class HttpError extends Error {
    constructor(status, message, details) {
        super(message);
        this.status = status;
        this.details = details;
    }
}

function route(handler, { auth = true } = {}) {
    return async (req, res, next) => {
        try {
            if (auth) {
                const token = (req.get('Authorization') || '').replace(/^Bearer\s+/i, '');
                const user = await authenticate(token);
                if (!user) throw new HttpError(401, 'unauthorized');
                req.user = user;
            }
            const data = await handler(req, res);
            if (!res.headersSent) res.json({ ok: true, data: data ?? null });
        } catch (e) {
            next(e);
        }
    };
}

function errorHandler(logger) {
    return (err, req, res, next) => {
        if (err.code === '23505') {
            return res.status(409).json({ ok: false, error: 'already exists', details: err.detail });
        }
        const status = err.status || 500;
        if (status >= 500) {
            logger.error(err.message, { stack: err.stack, path: req.path });
        }
        res.status(status).json({ ok: false, error: err.message, details: err.details });
    };
}

module.exports = { route, HttpError, errorHandler };
