const express = require('express');
const multer = require('multer');
const { z } = require('zod');
const { route, HttpError } = require('../core/route');
const { validate } = require('../core/validate');
const { query, one, tx } = require('../core/db');
const { uploadImage } = require('../core/upload');

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });

const NAME_RE = /^[a-zA-Z0-9а-яА-ЯёЁ\s\-.!#]+$/;

const createSchema = z.object({
    name: z.string().min(5).max(64).regex(NAME_RE, 'forbidden characters'),
    type: z.enum(['open', 'closed']).default('open'),
    about: z.string().max(500).optional(),
    rules: z.string().max(5000).optional(),
    filters: z.array(z.number().int()).optional(),
});

function parseBody(body) {
    if (body && typeof body.data === 'string') {
        try { return JSON.parse(body.data); }
        catch { throw new HttpError(400, 'invalid JSON in "data"'); }
    }
    return body;
}

// GET /groups/check-name?name=&excludeId= -- свободно ли имя (excludeId -- при редактировании своей же группы).
router.get('/groups/check-name', route(async (req) => {
    const name = String(req.query.name || '');
    const excludeId = req.query.excludeId || null;
    if (name.length < 5) return { available: false, reason: 'Название от 5 символов' };
    if (!NAME_RE.test(name)) return { available: false, reason: 'Запрещённые символы' };
    const taken = await one(
        'SELECT 1 FROM "group" WHERE name = $1 AND ($2::bigint IS NULL OR id != $2)',
        [name, excludeId]
    );
    return { available: !taken, reason: taken ? 'Такое имя уже занято' : null };
}));

// GET /groups?filter=<имя фильтра>&after=<id>&name=<поиск>&sort=<сортировка> -- список с числом участников.
router.get('/groups', route(async (req) => {
    const filter = req.query.filter || null;
    const after = req.query.after || null;
    const name = req.query.name || null;
    const sort = req.query.sort || null;
    const limit = 20;

    const orderBy =
        sort === 'members_desc' ? 'members_count DESC, g.id' :
        sort === 'members_asc'  ? 'members_count ASC, g.id' :
        sort === 'date_desc'    ? 'g.created_at DESC, g.id' :
        sort === 'date_asc'     ? 'g.created_at ASC, g.id' :
        'g.id';

    const rows = await query(
        `SELECT g.*,
                u.username AS creator_username,
                (SELECT COUNT(*) FROM group_member gm WHERE gm.group_id = g.id) AS members_count
         FROM "group" g
         JOIN app_user u ON u.id = g.creator_id
         WHERE ($1::text IS NULL OR EXISTS (
                  SELECT 1 FROM group_filter gf
                  JOIN filter f ON f.id = gf.filter_id
                  WHERE gf.group_id = g.id AND f.name = $1))
           AND ($2::bigint IS NULL OR g.id > $2)
           AND ($3::text IS NULL OR LOWER(g.name) LIKE '%' || LOWER($3) || '%')
         ORDER BY ${orderBy}
         LIMIT $4`,
        [filter, after, name, limit + 1]
    );
    return { items: rows.slice(0, limit), hasMore: rows.length > limit };
}));

// GET /groups/:groupId -- карточка (+ моя роль, + число участников).
router.get('/groups/:groupId', route(async (req) => {
    const group = await one(
        `SELECT g.*,
                u.username AS creator_username,
                (SELECT COUNT(*) FROM group_member gm WHERE gm.group_id = g.id) AS members_count,
                (SELECT role FROM group_member gm WHERE gm.group_id = g.id AND gm.user_id = $2) AS my_role
         FROM "group" g
         JOIN app_user u ON u.id = g.creator_id
         WHERE g.id = $1`,
        [req.params.groupId, req.user.id]
    );
    if (!group) throw new HttpError(404, 'group not found');
    return group;
}));

// POST /groups -- создать (нужно 100 очков опыта). Создатель становится 'creator'.
router.post('/groups', upload.single('image'), route(async (req) => {
    const data = validate(createSchema, parseBody(req.body));

    if (req.user.experience < 100) {
        throw new HttpError(403, 'Получите 100 очков опыта, чтобы создавать группы');
    }
    const taken = await one('SELECT 1 FROM "group" WHERE name = $1', [data.name]);
    if (taken) throw new HttpError(409, 'Группа с таким именем уже существует');

    const imageKey = await uploadImage(req.file, 'groups');

    return tx(async (client) => {
        const { rows: [group] } = await client.query(
            `INSERT INTO "group" (name, creator_id, type, about, rules, rules_image_key)
             VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
            [data.name, req.user.id, data.type, data.about ?? null, data.rules ?? null, imageKey]
        );
        await client.query(
            `INSERT INTO group_member (group_id, user_id, role) VALUES ($1, $2, 'creator')`,
            [group.id, req.user.id]
        );
        if (data.filters?.length) {
            await client.query(
                `INSERT INTO group_filter (group_id, filter_id) SELECT $1, unnest($2::smallint[])`,
                [group.id, data.filters]
            );
        }
        return group;
    });
}));

// PATCH /groups/:groupId -- редактировать (только создатель).
router.patch('/groups/:groupId', upload.single('image'), route(async (req) => {
    const groupId = req.params.groupId;
    const group = await one('SELECT creator_id FROM "group" WHERE id = $1', [groupId]);
    if (!group) throw new HttpError(404, 'group not found');
    if (group.creator_id !== req.user.id) throw new HttpError(403, 'only creator can edit');

    const data = validate(createSchema, parseBody(req.body));

    const taken = await one('SELECT 1 FROM "group" WHERE name = $1 AND id != $2', [data.name, groupId]);
    if (taken) throw new HttpError(409, 'Группа с таким именем уже существует');

    const imageKey = req.file ? await uploadImage(req.file, 'groups') : undefined;

    return tx(async (client) => {
        const { rows: [updated] } = await client.query(
            `UPDATE "group" SET name = $2, type = $3, about = $4, rules = $5,
                    rules_image_key = COALESCE($6, rules_image_key)
             WHERE id = $1 RETURNING *`,
            [groupId, data.name, data.type, data.about ?? null, data.rules ?? null, imageKey ?? null]
        );
        await client.query('DELETE FROM group_filter WHERE group_id = $1', [groupId]);
        if (data.filters?.length) {
            await client.query(
                `INSERT INTO group_filter (group_id, filter_id) SELECT $1, unnest($2::smallint[])`,
                [groupId, data.filters]
            );
        }
        return updated;
    });
}));

// DELETE /groups/:groupId -- удалить (только создатель).
router.delete('/groups/:groupId', route(async (req) => {
    const group = await one('SELECT creator_id FROM "group" WHERE id = $1', [req.params.groupId]);
    if (!group) throw new HttpError(404, 'group not found');
    if (group.creator_id !== req.user.id) throw new HttpError(403, 'only creator can delete');

    await one('DELETE FROM "group" WHERE id = $1 RETURNING id', [req.params.groupId]);
    return { deleted: true };
}));

module.exports = router;
