const { app, request, pool, reset, register, authHeader, setExp } = require('./helpers');

beforeEach(reset);
afterAll(() => pool.end());

const makeGroup = (token, name = 'EcoClub') =>
    request(app).post('/groups').set(authHeader(token)).send({ name, type: 'open', about: 't' });

describe('groups', () => {
    test('create requires 100 exp', async () => {
        const { token } = await register();
        expect((await makeGroup(token)).status).toBe(403);
    });

    test('create → creator membership, post, list with JOIN enrichment', async () => {
        const { token, user } = await register();
        await setExp(user.id, 150);

        const g = await makeGroup(token);
        expect(g.status).toBe(200);
        const gid = g.body.data.id;

        const p = await request(app).post(`/groups/${gid}/posts`).set(authHeader(token)).send({ text: 'hi' });
        expect(p.status).toBe(200);

        const list = await request(app).get(`/groups/${gid}/posts`).set(authHeader(token));
        expect(list.body.data.items).toHaveLength(1);
        expect(list.body.data.items[0].author_username).toBe(user.username);
        expect(list.body.data.items[0].author_role).toBe('creator');
        expect(list.body.data.items[0].comments_count).toBe(0);
    });

    test('non-member cannot post (403)', async () => {
        const a = await register();
        await setExp(a.user.id, 150);
        const gid = (await makeGroup(a.token)).body.data.id;

        const b = await register();
        const res = await request(app).post(`/groups/${gid}/posts`).set(authHeader(b.token)).send({ text: 'x' });
        expect(res.status).toBe(403);
    });

    test('creator can moderate (delete) a member post', async () => {
        const a = await register();
        await setExp(a.user.id, 150);
        const gid = (await makeGroup(a.token)).body.data.id;

        const b = await register();
        await request(app).post(`/groups/${gid}/members`).set(authHeader(b.token));
        const pid = (await request(app).post(`/groups/${gid}/posts`).set(authHeader(b.token)).send({ text: 'bob' })).body.data.id;

        const del = await request(app).delete(`/posts/${pid}`).set(authHeader(a.token));
        expect(del.status).toBe(200);
    });

    test('duplicate group name → 409', async () => {
        const a = await register();
        await setExp(a.user.id, 150);
        await makeGroup(a.token, 'Unique Name');
        const dup = await makeGroup(a.token, 'Unique Name');
        expect(dup.status).toBe(409);
    });
});
