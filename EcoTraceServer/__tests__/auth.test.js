const { app, request, pool, reset, register, authHeader } = require('./helpers');

beforeEach(reset);
afterAll(() => pool.end());

describe('auth', () => {
    test('register returns user + token, hides password_hash', async () => {
        const { res } = await register({ username: 'alice', email: 'alice@e.com' });
        expect(res.status).toBe(200);
        expect(res.body.ok).toBe(true);
        expect(res.body.data.user.username).toBe('alice');
        expect(typeof res.body.data.token).toBe('string');
        expect(res.body.data.user.password_hash).toBeUndefined();
    });

    test('duplicate username → 409', async () => {
        await register({ username: 'bob', email: 'bob1@e.com' });
        const { res } = await register({ username: 'bob', email: 'bob2@e.com' });
        expect(res.status).toBe(409);
    });

    test('login by username/email, wrong password → 401', async () => {
        const { user } = await register({ password: 'secret123' });
        const ok = await request(app).post('/login').send({ login: user.username, password: 'secret123' });
        expect(ok.status).toBe(200);
        const byEmail = await request(app).post('/login').send({ login: user.email, password: 'secret123' });
        expect(byEmail.status).toBe(200);
        const bad = await request(app).post('/login').send({ login: user.username, password: 'nope' });
        expect(bad.status).toBe(401);
    });

    test('/me requires a valid token', async () => {
        const { token } = await register();
        expect((await request(app).get('/me')).status).toBe(401);
        const ok = await request(app).get('/me').set(authHeader(token));
        expect(ok.status).toBe(200);
        expect(typeof ok.body.data.email).toBe('string');
    });

    test('validation error → 400', async () => {
        const res = await request(app).post('/register').send({ username: 'x', email: 'bad', password: '1' });
        expect(res.status).toBe(400);
    });
});
