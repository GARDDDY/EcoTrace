const { app, request, pool, reset, register, authHeader, setExp } = require('./helpers');

beforeEach(reset);
afterAll(() => pool.end());

const makeEvent = (token) =>
    request(app).post('/events').set(authHeader(token)).send({
        name: 'Cleanup',
        about: 'x',
        schedule: [{ title: 'A', starts_at: '2030-07-01T09:00:00Z', ends_at: '2030-07-01T12:00:00Z' }],
        goals: ['g1'],
    });

describe('events', () => {
    test('create requires 50 exp', async () => {
        const { token } = await register();
        expect((await makeEvent(token)).status).toBe(403);
    });

    test('create → card shows creator/timing; join; validate awards exp', async () => {
        const a = await register();
        await setExp(a.user.id, 60);
        const ev = await makeEvent(a.token);
        expect(ev.status).toBe(200);
        const eid = ev.body.data.id;

        const card = await request(app).get(`/events/${eid}`).set(authHeader(a.token));
        expect(card.body.data.my_role).toBe('creator');
        expect(card.body.data.members_count).toBe(1);
        expect(card.body.data.timing).toBe(0); // в будущем

        const goals = await request(app).get(`/events/${eid}/goals`).set(authHeader(a.token));
        expect(goals.body.data).toHaveLength(1);

        const b = await register();
        expect((await request(app).post(`/events/${eid}/members`).set(authHeader(b.token))).status).toBe(200);

        // участник не может валидировать сам себя
        const self = await request(app).post(`/events/${eid}/members/${b.user.id}/validate`).set(authHeader(b.token));
        expect(self.status).toBe(403);

        // создатель валидирует → +10 опыта
        const val = await request(app).post(`/events/${eid}/members/${b.user.id}/validate`).set(authHeader(a.token));
        expect(val.body.data.awarded).toBe(10);

        const bob = await request(app).get(`/users/${b.user.id}`).set(authHeader(b.token));
        expect(bob.body.data.experience).toBe(10);
    });
});
