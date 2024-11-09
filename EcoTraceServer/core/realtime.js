const { Server } = require('socket.io');
const { authenticate } = require('./auth');
const { one } = require('./db');
const logger = require('../logger');

let io = null;

async function assertCanSubscribe(channel, user) {
    const [kind, id, sub] = String(channel).split(':');

    if (kind === 'group' && sub === 'posts') {
        const g = await one('SELECT type FROM "group" WHERE id = $1', [id]);
        if (!g) throw new Error('group not found');
        if (g.type === 'closed') {
            const m = await one('SELECT 1 FROM group_member WHERE group_id = $1 AND user_id = $2', [id, user.id]);
            if (!m) throw new Error('forbidden');
        }
        return;
    }

    if (kind === 'post' && sub === 'comments') {
        const p = await one(
            'SELECT g.id AS gid, g.type FROM post p JOIN "group" g ON g.id = p.group_id WHERE p.id = $1',
            [id]
        );
        if (!p) throw new Error('post not found');
        if (p.type === 'closed') {
            const m = await one('SELECT 1 FROM group_member WHERE group_id = $1 AND user_id = $2', [p.gid, user.id]);
            if (!m) throw new Error('forbidden');
        }
        return;
    }

    throw new Error('unknown channel');
}

function init(httpServer) {
    io = new Server(httpServer, { cors: { origin: '*' } });

    io.use(async (socket, next) => {
        try {
            const user = await authenticate(socket.handshake.auth && socket.handshake.auth.token);
            if (!user) return next(new Error('unauthorized'));
            socket.user = user;
            next();
        } catch {
            next(new Error('unauthorized'));
        }
    });

    io.on('connection', (socket) => {
        socket.on('subscribe', async (channel, ack) => {
            try {
                await assertCanSubscribe(channel, socket.user);
                socket.join(channel);
                if (typeof ack === 'function') ack({ ok: true });
            } catch (e) {
                if (typeof ack === 'function') ack({ ok: false, error: e.message });
            }
        });

        socket.on('unsubscribe', (channel) => socket.leave(String(channel)));
    });

    logger.info('Realtime (socket.io) initialised');
    return io;
}

function publish(channel, event, payload) {
    if (io) io.to(channel).emit(event, payload);
}

module.exports = { init, publish };
