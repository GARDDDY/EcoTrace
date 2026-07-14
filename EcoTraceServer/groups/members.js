const express = require('express');
const { z } = require('zod');
const { route, HttpError } = require('../core/route');
const { validate } = require('../core/validate');
const { query, one } = require('../core/db');
const { ROLE_RANK, membership, requireMember, requireRole } = require('./access');

const router = express.Router();

// GET /groups/:groupId/members?role=&after=&name=&sort=posts_desc -- список участников.
router.get('/groups/:groupId/members', route(async (req) => {
    const { groupId } = req.params;
    const role = req.query.role || null;
    const after = req.query.after || null;
    const name = req.query.name || null;
    const sort = req.query.sort || null;
    const limit = Math.min(Number(req.query.limit) || 30, 100);

    const orderBy = sort === 'posts_desc' ? 'posts_count DESC, gm.user_id' : 'gm.user_id';

    const rows = await query(
        `SELECT gm.user_id, gm.role, gm.joined_at, u.username, u.avatar_key,
                (SELECT COUNT(*) FROM post p WHERE p.group_id = gm.group_id AND p.author_id = gm.user_id) AS posts_count
         FROM group_member gm
         JOIN app_user u ON u.id = gm.user_id
         WHERE gm.group_id = $1
           AND ($2::group_role IS NULL OR gm.role = $2)
           AND ($3::bigint IS NULL OR gm.user_id > $3)
           AND ($4::text IS NULL OR LOWER(u.username) LIKE '%' || LOWER($4) || '%')
         ORDER BY ${orderBy}
         LIMIT $5`,
        [groupId, role, after, name, limit + 1]
    );
    const total = await one(
        'SELECT COUNT(*)::int AS count FROM group_member WHERE group_id = $1 AND ($2::group_role IS NULL OR role = $2)',
        [groupId, role]
    );
    return { items: rows.slice(0, limit), has_more: rows.length > limit, total: total.count };
}));

// POST /groups/:groupId/members -- вступить самому.
router.post('/groups/:groupId/members', route(async (req) => {
    const { groupId } = req.params;
    const group = await one('SELECT id FROM "group" WHERE id = $1', [groupId]);
    if (!group) throw new HttpError(404, 'group not found');
    if (await membership(groupId, req.user.id)) throw new HttpError(409, 'already a member');

    await one(
        `INSERT INTO group_member (group_id, user_id, role) VALUES ($1, $2, 'participant')`,
        [groupId, req.user.id]
    );
    return { joined: true };
}));

// DELETE /groups/:groupId/members/me -- выйти. Создатель выйти не может.
router.delete('/groups/:groupId/members/me', route(async (req) => {
    const m = await requireMember(req.params.groupId, req.user.id);
    if (m.role === 'creator') throw new HttpError(403, 'creator cannot leave');

    await one('DELETE FROM group_member WHERE group_id = $1 AND user_id = $2 RETURNING user_id',
        [req.params.groupId, req.user.id]);
    return { left: true };
}));

// PATCH /groups/:groupId/members/:userId -- назначить роль (руководитель+, строго выше цели).
const roleSchema = z.object({ role: z.enum(['participant', 'assistant', 'manager']) });
router.patch('/groups/:groupId/members/:userId', route(async (req) => {
    const { groupId, userId } = req.params;
    const { role } = validate(roleSchema, req.body);

    const actor = await requireRole(groupId, req.user.id, 'manager');
    const target = await membership(groupId, userId);
    if (!target) throw new HttpError(404, 'member not found');
    if (target.role === 'creator') throw new HttpError(403, 'cannot change creator');
    if (ROLE_RANK[actor.role] <= ROLE_RANK[target.role]) throw new HttpError(403, 'cannot manage equal/higher role');

    await one('UPDATE group_member SET role = $1 WHERE group_id = $2 AND user_id = $3 RETURNING user_id',
        [role, groupId, userId]);
    return { updated: true };
}));

// DELETE /groups/:groupId/members/:userId -- исключить (руководитель+, строго выше цели).
router.delete('/groups/:groupId/members/:userId', route(async (req) => {
    const { groupId, userId } = req.params;

    const actor = await requireRole(groupId, req.user.id, 'manager');
    const target = await membership(groupId, userId);
    if (!target) throw new HttpError(404, 'member not found');
    if (target.role === 'creator') throw new HttpError(403, 'cannot kick creator');
    if (ROLE_RANK[actor.role] <= ROLE_RANK[target.role]) throw new HttpError(403, 'cannot kick equal/higher role');

    await one('DELETE FROM group_member WHERE group_id = $1 AND user_id = $2 RETURNING user_id',
        [groupId, userId]);
    return { kicked: true };
}));

module.exports = router;
