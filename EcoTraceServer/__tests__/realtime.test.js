const http = require('http');
const { io: ioClient } = require('socket.io-client');
const { app, request, pool, reset, register, setExp, authHeader } = require('./helpers');
const realtime = require('../core/realtime');

let server;
let port;

beforeAll((done) => {
    server = http.createServer(app);
    realtime.init(server);
    server.listen(0, () => { port = server.address().port; done(); });
});

afterAll(async () => {
    await new Promise((res) => server.close(res));
    await pool.end();
});

beforeEach(reset);

function connect(token) {
    const client = ioClient(`http://localhost:${port}`, { auth: { token }, forceNew: true, transports: ['websocket'] });
    return new Promise((res, rej) => {
        client.on('connect', () => res(client));
        client.on('connect_error', rej);
    });
}

describe('realtime observer', () => {
    test('rejects connection without a valid token', async () => {
        await expect(connect('garbage')).rejects.toThrow();
    });

    test('subscriber receives post:new for its group', async () => {
        const a = await register();
        await setExp(a.user.id, 150);
        const gid = (await request(app).post('/groups').set(authHeader(a.token)).send({ name: 'RT Club', type: 'open' })).body.data.id;

        const client = await connect(a.token);
        const sub = await new Promise((res) => client.emit('subscribe', `group:${gid}:posts`, res));
        expect(sub.ok).toBe(true);

        const received = new Promise((res, rej) => {
            client.on('post:new', res);
            setTimeout(() => rej(new Error('no event')), 4000);
        });

        await request(app).post(`/groups/${gid}/posts`).set(authHeader(a.token)).send({ text: 'live' });

        const evt = await received;
        expect(evt.text).toBe('live');
        expect(evt.author_username).toBe(a.user.username);
        expect(evt.author_role).toBe('creator');
        client.disconnect();
    });

    test('subscribe to a closed group requires membership', async () => {
        const owner = await register();
        await setExp(owner.user.id, 150);
        const gid = (await request(app).post('/groups').set(authHeader(owner.token)).send({ name: 'Secret', type: 'closed' })).body.data.id;

        const outsider = await register();
        const client = await connect(outsider.token);
        const sub = await new Promise((res) => client.emit('subscribe', `group:${gid}:posts`, res));
        expect(sub.ok).toBe(false);
        client.disconnect();
    });
});
