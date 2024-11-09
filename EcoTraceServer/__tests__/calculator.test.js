const { app, request, pool, reset, register, authHeader } = require('./helpers');

beforeEach(reset);
afterAll(() => pool.end());

describe('calculator', () => {
    test('list calculators (public) with questions', async () => {
        const res = await request(app).get('/calculators');
        expect(res.status).toBe(200);
        const food = res.body.data.find((c) => c.id === 0);
        expect(food.name).toBe('Пища');
        expect(food.questions.length).toBeGreaterThan(0);
    });

    test('entry awards exp once/day; averages computed from JSONB', async () => {
        const { token } = await register();

        const first = await request(app).post('/calculators/0/entries').set(authHeader(token))
            .send({ answers: [{ value: 0, specify: { '0': 120, '2': 80 } }, { value: 300 }] });
        expect(first.status).toBe(200);
        expect(first.body.data.awarded).toBe(2);

        const state = await request(app).get('/calculators/0/state').set(authHeader(token));
        expect(state.body.data.first).toBe(false);
        expect(state.body.data.updating).toBe(true);
        const q0 = state.body.data.averages.find((a) => a.question === 0);
        expect(q0.value).toBe(200); // 120 + 80

        const again = await request(app).post('/calculators/0/entries').set(authHeader(token)).send({ answers: [{ value: 10 }] });
        expect(again.body.data.awarded).toBe(0);
    });

    test('graph returns aligned data arrays, not an image', async () => {
        const { token, user } = await register();
        await request(app).post('/calculators/0/entries').set(authHeader(token)).send({ answers: [{ value: 100 }] });

        const g = await request(app).get(`/users/${user.id}/graph`).set(authHeader(token));
        expect(g.status).toBe(200);
        expect(g.headers['content-type']).toMatch(/json/);
        expect(g.body.data.days).toHaveLength(7);
        expect(Array.isArray(g.body.data.datasets)).toBe(true);
    });
});
