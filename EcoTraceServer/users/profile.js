const express = require('express');
const multer = require('multer');
const { z } = require('zod');
const { route, HttpError } = require('../core/route');
const { validate } = require('../core/validate');
const { query, one, tx } = require('../core/db');
const { uploadImage } = require('../core/upload');
const { hashPassword, verifyPassword } = require('../core/auth');
const { areFriends, canSee } = require('./access');

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });

const USER_FIELDS = `id, username, email, fullname, gender, about_me, country_code, experience,
    avatar_key, name_visibility, country_visibility, friends_visibility, groups_visibility, created_at`;

const visEnum = z.enum(['everyone', 'friends', 'nobody']);

const editSchema = z.object({
    username: z.string().min(3).max(32).optional(),
    about_me: z.string().max(500).nullable().optional(),
    country_code: z.string().length(2).nullable().optional(),
    fullname: z.string().max(64).nullable().optional(),
    filters: z.array(z.number().int()).optional(),
});

const privacySchema = z.object({
    name_visibility: visEnum.optional(),
    country_visibility: visEnum.optional(),
    friends_visibility: visEnum.optional(),
    groups_visibility: visEnum.optional(),
});

function parseBody(body) {
    if (body && typeof body.data === 'string') {
        try { return JSON.parse(body.data); }
        catch { throw new HttpError(400, 'invalid JSON in "data"'); }
    }
    return body || {};
}

// GET /users?q=&filter=&sort=rating&common_group=true&common_event=true&after= -- поиск пользователей (для добавления в друзья).
router.get('/users', route(async (req) => {
    const q = req.query.q || null;
    const filter = req.query.filter || null;
    const sort = req.query.sort || null;
    const commonGroup = req.query.common_group === 'true' ? true : null;
    const commonEvent = req.query.common_event === 'true' ? true : null;
    const after = req.query.after || null;
    const limit = 20;

    const orderBy = sort === 'rating' ? 'u.experience DESC, u.id' : 'u.id';

    const rows = await query(
        `SELECT u.id, u.username, u.avatar_key, u.experience, u.country_code,
                (SELECT COALESCE(array_agg(filter_id), '{}') FROM user_filter WHERE user_id = u.id) AS filters,
                (SELECT COUNT(*) FROM group_member gm1 JOIN group_member gm2 ON gm1.group_id = gm2.group_id
                    WHERE gm1.user_id = $1 AND gm2.user_id = u.id) AS common_groups_count,
                (SELECT COUNT(*) FROM event_member em1 JOIN event_member em2 ON em1.event_id = em2.event_id
                    WHERE em1.user_id = $1 AND em2.user_id = u.id) AS common_events_count
         FROM app_user u
         WHERE u.id <> $1
           AND ($2::text IS NULL OR u.username ILIKE $2 || '%')
           AND ($3::text IS NULL OR EXISTS (
                SELECT 1 FROM user_filter uf JOIN filter f ON f.id = uf.filter_id
                WHERE uf.user_id = u.id AND f.name = $3))
           AND ($4::bigint IS NULL OR u.id > $4)
           AND ($5::boolean IS NULL OR EXISTS (
                SELECT 1 FROM group_member gm1 JOIN group_member gm2 ON gm1.group_id = gm2.group_id
                WHERE gm1.user_id = $1 AND gm2.user_id = u.id))
           AND ($6::boolean IS NULL OR EXISTS (
                SELECT 1 FROM event_member em1 JOIN event_member em2 ON em1.event_id = em2.event_id
                WHERE em1.user_id = $1 AND em2.user_id = u.id))
         ORDER BY ${orderBy}
         LIMIT $7`,
        [req.user.id, q, filter, after, commonGroup, commonEvent, limit + 1]
    );
    return { items: rows.slice(0, limit), has_more: rows.length > limit };
}));

// GET /users/:id -- карточка профиля (id или "me"). Применяет приватность.
router.get('/users/:id', route(async (req) => {
    const targetId = req.params.id === 'me' ? req.user.id : req.params.id;

    const u = await one(
        `SELECT ${USER_FIELDS},
                (SELECT COALESCE(array_agg(filter_id), '{}') FROM user_filter WHERE user_id = app_user.id) AS filters
         FROM app_user WHERE id = $1`,
        [targetId]
    );
    if (!u) throw new HttpError(404, 'user not found');

    const isOwner = String(u.id) === String(req.user.id);
    const isFriend = isOwner ? true : await areFriends(req.user.id, u.id);

    if (!canSee(u.name_visibility, isOwner, isFriend)) u.fullname = null;
    if (!canSee(u.country_visibility, isOwner, isFriend)) u.country_code = null;

    u.is_owner = isOwner;
    u.is_friend = isFriend && !isOwner;
    if (!isOwner) delete u.email;
    return u;
}));

// PATCH /users/me -- изменить профиль (+ аватар через multipart).
router.patch('/users/me', upload.single('image'), route(async (req) => {
    const data = validate(editSchema, parseBody(req.body));
    const avatarKey = await uploadImage(req.file, 'users');

    return tx(async (client) => {
        const sets = [];
        const params = [];
        let i = 1;
        for (const c of ['username', 'about_me', 'country_code', 'fullname']) {
            if (c in data) { sets.push(`${c} = $${i++}`); params.push(data[c]); }
        }
        if (avatarKey) { sets.push(`avatar_key = $${i++}`); params.push(avatarKey); }
        if (sets.length) {
            params.push(req.user.id);
            await client.query(`UPDATE app_user SET ${sets.join(', ')} WHERE id = $${i}`, params);
        }
        if (data.filters) {
            await client.query('DELETE FROM user_filter WHERE user_id = $1', [req.user.id]);
            if (data.filters.length) {
                await client.query(
                    'INSERT INTO user_filter (user_id, filter_id) SELECT $1, unnest($2::smallint[])',
                    [req.user.id, data.filters]
                );
            }
        }
        const { rows } = await client.query(`SELECT ${USER_FIELDS} FROM app_user WHERE id = $1`, [req.user.id]);
        return rows[0];
    });
}));

// PATCH /users/me/privacy -- настройки видимости.
router.patch('/users/me/privacy', route(async (req) => {
    const data = validate(privacySchema, req.body);
    const sets = [];
    const params = [];
    let i = 1;
    for (const c of Object.keys(data)) { sets.push(`${c} = $${i++}`); params.push(data[c]); }
    if (!sets.length) throw new HttpError(400, 'nothing to update');
    params.push(req.user.id);
    await one(`UPDATE app_user SET ${sets.join(', ')} WHERE id = $${i} RETURNING id`, params);
    return { updated: true };
}));

// PUT /users/me/password -- смена пароля (нужен текущий).
const passwordSchema = z.object({ current: z.string().min(1), next: z.string().min(6) });
router.put('/users/me/password', route(async (req) => {
    const { current, next } = validate(passwordSchema, req.body);
    const u = await one('SELECT password_hash FROM app_user WHERE id = $1', [req.user.id]);
    if (!(await verifyPassword(current, u.password_hash))) throw new HttpError(403, 'wrong current password');
    await one('UPDATE app_user SET password_hash = $1 WHERE id = $2 RETURNING id',
        [await hashPassword(next), req.user.id]);
    return { changed: true };
}));

// PUT /users/me/email -- смена почты (подтверждение паролем; UNIQUE-конфликт → 409).
const emailSchema = z.object({ email: z.string().email(), password: z.string().min(1) });
router.put('/users/me/email', route(async (req) => {
    const { email, password } = validate(emailSchema, req.body);
    const u = await one('SELECT password_hash FROM app_user WHERE id = $1', [req.user.id]);
    if (!(await verifyPassword(password, u.password_hash))) throw new HttpError(403, 'wrong password');
    await one('UPDATE app_user SET email = $1 WHERE id = $2 RETURNING id', [email, req.user.id]);
    return { changed: true };
}));

module.exports = router;
