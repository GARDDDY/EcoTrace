const { app, request, pool, reset, register, setExp } = require('./helpers');

beforeEach(reset);
afterAll(() => pool.end());

describe('app / pages / news / rating', () => {
    test('home and ping respond', async () => {
        expect((await request(app).get('/')).status).toBe(200);
        expect((await request(app).get('/ping')).status).toBe(200);
    });

    test('rating sorted by experience (public)', async () => {
        const a = await register();
        await setExp(a.user.id, 60);
        const b = await register();
        await setExp(b.user.id, 10);

        const r = await request(app).get('/rating');
        expect(r.status).toBe(200);
        expect(r.body.data.items[0].experience).toBeGreaterThanOrEqual(r.body.data.items[1].experience);
    });

    test('news cache empty by default', async () => {
        const r = await request(app).get('/news');
        expect(r.status).toBe(200);
        expect(r.body.data).toEqual([]);
    });

    test('constants returns event roles', async () => {
        const r = await request(app).get('/constants?type=0');
        expect(r.body).toEqual(['Создатель', 'Помощник', 'Участник']);
    });

    test('serves rank image from local uploads', async () => {
        const r = await request(app).get('/uploads/$ranks/rank1');
        expect(r.status).toBe(200);
        expect(r.headers['content-type']).toMatch(/image/);
    });
});
