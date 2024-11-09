const express = require('express');
const { route, HttpError } = require('../core/route');
const { query, one } = require('../core/db');
const { areFriends, canSee } = require('./access');
const { timing } = require('../events/access');

const router = express.Router();

// GET /users/:id/groups?sort=members|role -- группы пользователя (с учётом groups_visibility).
router.get('/users/:id/groups', route(async (req) => {
    const targetId = req.params.id === 'me' ? req.user.id : req.params.id;
    const after = req.query.after || null;
    const limit = 20;
    const orderBy = req.query.sort === 'members' ? 'members_count DESC, g.id' : 'gm.role DESC, g.id';

    const target = await one('SELECT id, groups_visibility FROM app_user WHERE id = $1', [targetId]);
    if (!target) throw new HttpError(404, 'user not found');

    const isOwner = String(target.id) === String(req.user.id);
    const isFriend = isOwner ? true : await areFriends(req.user.id, target.id);
    if (!canSee(target.groups_visibility, isOwner, isFriend)) {
        return { items: [], hasMore: false, hidden: true };
    }

    const rows = await query(
        `SELECT g.id, g.name, g.about, g.rules_image_key, gm.role,
                (SELECT COUNT(*) FROM group_member m WHERE m.group_id = g.id) AS members_count
         FROM group_member gm
         JOIN "group" g ON g.id = gm.group_id
         WHERE gm.user_id = $1 AND ($2::bigint IS NULL OR g.id > $2)
         ORDER BY ${orderBy}
         LIMIT $3`,
        [target.id, after, limit + 1]
    );
    return { items: rows.slice(0, limit), hasMore: rows.length > limit };
}));

// GET /users/:id/events?sort=members|role -- мероприятия пользователя (видны всем).
router.get('/users/:id/events', route(async (req) => {
    const targetId = req.params.id === 'me' ? req.user.id : req.params.id;
    const after = req.query.after || null;
    const limit = 20;
    const orderBy = req.query.sort === 'members' ? 'members_count DESC, e.id' : 'em.role DESC, e.id';

    const rows = await query(
        `SELECT e.id, e.name, e.status, em.role, (em.validated_at IS NOT NULL) AS validated,
                (SELECT COUNT(*) FROM event_member m WHERE m.event_id = e.id) AS members_count,
                (SELECT MIN(starts_at) FROM event_schedule_item s WHERE s.event_id = e.id) AS starts_at,
                (SELECT MAX(ends_at)   FROM event_schedule_item s WHERE s.event_id = e.id) AS ends_at
         FROM event_member em
         JOIN event e ON e.id = em.event_id
         WHERE em.user_id = $1 AND ($2::bigint IS NULL OR e.id > $2)
         ORDER BY ${orderBy}
         LIMIT $3`,
        [targetId, after, limit + 1]
    );
    const items = rows.slice(0, limit).map(e => ({ ...e, timing: timing(e.starts_at, e.ends_at) }));
    return { items, hasMore: rows.length > limit };
}));

module.exports = router;
