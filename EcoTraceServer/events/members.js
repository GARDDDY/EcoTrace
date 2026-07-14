const express = require('express');
const { z } = require('zod');
const { route, HttpError } = require('../core/route');
const { validate } = require('../core/validate');
const { query, one, tx } = require('../core/db');
const { membership, requireMember, requireRole, timing } = require('./access');

const router = express.Router();
const VALIDATE_REWARD = 10;

// GET /events/:id/members?username=&after= -- список участников.
router.get('/events/:id/members', route(async (req) => {
    const uname = req.query.username || null;
    const after = req.query.after || null;
    const limit = Math.min(Number(req.query.limit) || 50, 100);

    const rows = await query(
        `SELECT em.user_id, em.role, (em.validated_at IS NOT NULL) AS validated,
                u.username, u.experience, u.avatar_key
         FROM event_member em
         JOIN app_user u ON u.id = em.user_id
         WHERE em.event_id = $1
           AND ($2::text IS NULL OR u.username ILIKE $2 || '%')
           AND ($3::bigint IS NULL OR em.user_id > $3)
         ORDER BY em.user_id
         LIMIT $4`,
        [req.params.id, uname, after, limit + 1]
    );
    return { items: rows.slice(0, limit), has_more: rows.length > limit };
}));

// GET /events/:id/members/preview -- до 5 случайных участников (только аватар, без имени) -- превью на карточке.
router.get('/events/:id/members/preview', route(async (req) => {
    const limit = Math.min(Number(req.query.limit) || 5, 20);
    const rows = await query(
        `SELECT em.user_id, em.role, (em.validated_at IS NOT NULL) AS validated,
                u.username, u.experience, u.avatar_key
         FROM event_member em
         JOIN app_user u ON u.id = em.user_id
         WHERE em.event_id = $1
         ORDER BY random()
         LIMIT $2`,
        [req.params.id, limit]
    );
    return { items: rows, has_more: false };
}));

// POST /events/:id/members -- вступить.
router.post('/events/:id/members', route(async (req) => {
    const eventId = req.params.id;
    if (!(await one('SELECT 1 FROM event WHERE id = $1', [eventId]))) throw new HttpError(404, 'event not found');
    if (await membership(eventId, req.user.id)) throw new HttpError(409, 'already joined');

    await one(`INSERT INTO event_member (event_id, user_id, role) VALUES ($1, $2, 'participant')`,
        [eventId, req.user.id]);
    return { joined: true };
}));

// DELETE /events/:id/members/me -- выйти (создатель не может; только пока мероприятие не началось).
router.delete('/events/:id/members/me', route(async (req) => {
    const m = await requireMember(req.params.id, req.user.id);
    if (m.role === 'creator') throw new HttpError(403, 'creator cannot leave');

    const sched = await one(
        `SELECT MIN(starts_at) AS starts_at, MAX(ends_at) AS ends_at
         FROM event_schedule_item WHERE event_id = $1`,
        [req.params.id]
    );
    if (timing(sched?.starts_at, sched?.ends_at) !== 0) {
        throw new HttpError(409, 'cannot leave an event that has already started or finished');
    }

    await one('DELETE FROM event_member WHERE event_id = $1 AND user_id = $2 RETURNING user_id',
        [req.params.id, req.user.id]);
    return { left: true };
}));

// PATCH /events/:id/members/:userId -- назначить роль (только создатель).
const roleSchema = z.object({ role: z.enum(['participant', 'assistant']) });
router.patch('/events/:id/members/:userId', route(async (req) => {
    const { role } = validate(roleSchema, req.body);
    await requireRole(req.params.id, req.user.id, 'creator');

    const target = await membership(req.params.id, req.params.userId);
    if (!target) throw new HttpError(404, 'member not found');
    if (target.role === 'creator') throw new HttpError(403, 'cannot change creator');

    await one('UPDATE event_member SET role = $1 WHERE event_id = $2 AND user_id = $3 RETURNING user_id',
        [role, req.params.id, req.params.userId]);
    return { updated: true };
}));

// POST /events/:id/members/:userId/validate -- подтвердить присутствие по QR (помощник+), начислить опыт один раз.
router.post('/events/:id/members/:userId/validate', route(async (req) => {
    await requireRole(req.params.id, req.user.id, 'assistant');

    const target = await membership(req.params.id, req.params.userId);
    if (!target) throw new HttpError(404, 'member not found');
    if (target.validated_at) return { validated: true, awarded: 0, message: 'already validated' };

    return tx(async (client) => {
        await client.query('UPDATE event_member SET validated_at = now() WHERE event_id = $1 AND user_id = $2',
            [req.params.id, req.params.userId]);
        await client.query(
            `INSERT INTO experience_ledger (user_id, amount, reason, ref_id) VALUES ($1, $2, 'event', $3)`,
            [req.params.userId, VALIDATE_REWARD, req.params.id]
        );
        await client.query('UPDATE app_user SET experience = experience + $1 WHERE id = $2',
            [VALIDATE_REWARD, req.params.userId]);
        return { validated: true, awarded: VALIDATE_REWARD };
    });
}));

module.exports = router;
