const express = require('express');
const multer = require('multer');
const { z } = require('zod');
const { route, HttpError } = require('../core/route');
const { validate } = require('../core/validate');
const { query, one, tx } = require('../core/db');
const { uploadImage } = require('../core/upload');
const { timing } = require('./access');

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });

const createSchema = z.object({
    name: z.string().min(3).max(120),
    about: z.string().max(2000).optional(),
    filters: z.array(z.string().min(1)).optional(),
    start_condition: z.enum(['first_action', 'member_count']).default('first_action'),
    start_threshold: z.number().int().positive().optional(),
    schedule: z.array(z.object({
        title: z.string().min(1),
        starts_at: z.string(),
        ends_at: z.string().optional(),
    })).min(1),
    goals: z.array(z.string().min(1)).optional(),
    zones: z.array(z.object({
        label: z.string().optional(),
        is_zone: z.boolean().optional(),
        latitude: z.number(),
        longitude: z.number(),
        radius_m: z.number().int().optional(),
        color_fill: z.string().optional(),
        color_stroke: z.string().optional(),
        schedule_index: z.number().int().optional(),
    })).optional(),
});

function parseBody(body) {
    if (body && typeof body.data === 'string') {
        try { return JSON.parse(body.data); }
        catch { throw new HttpError(400, 'invalid JSON in "data"'); }
    }
    return body || {};
}

// GET /events?filter=&from=&to=&after=&name=&sort=&min_members=&max_members=&has_map=&lat=&lng=&radius_km= -- список.
router.get('/events', route(async (req) => {
    const filter = req.query.filter || null;
    const from = req.query.from || null;
    const to = req.query.to || null;
    const after = req.query.after || null;
    const name = req.query.name || null;
    const sort = req.query.sort || null;
    const minMembers = req.query.min_members != null ? Number(req.query.min_members) : null;
    const maxMembers = req.query.max_members != null ? Number(req.query.max_members) : null;
    const hasMap = req.query.has_map != null ? req.query.has_map === 'true' : null;
    const lat = req.query.lat != null ? Number(req.query.lat) : null;
    const lng = req.query.lng != null ? Number(req.query.lng) : null;
    const radiusKm = req.query.radius_km != null ? Number(req.query.radius_km) : null;
    const limit = 20;
    const orderBy =
        sort === 'members_desc' ? 'members_count DESC, e.id' :
        sort === 'members_asc'  ? 'members_count ASC, e.id' :
        sort === 'date_desc'    ? 'starts_at DESC NULLS LAST, e.id' :
        'starts_at ASC NULLS LAST, e.id';

    const rows = await query(
        `SELECT e.id, e.name, e.about, e.status, e.image_key, e.created_at,
                u.username AS creator_username,
                (SELECT COUNT(*) FROM event_member m WHERE m.event_id = e.id) AS members_count,
                (SELECT MIN(starts_at) FROM event_schedule_item s WHERE s.event_id = e.id) AS starts_at,
                (SELECT MAX(ends_at)   FROM event_schedule_item s WHERE s.event_id = e.id) AS ends_at,
                EXISTS (SELECT 1 FROM event_zone z WHERE z.event_id = e.id) AS has_map,
                (SELECT COALESCE(json_agg(json_build_object(
                            'name', f.name, 'color_fg', f.color_fg, 'color_bg', f.color_bg
                        ) ORDER BY f.id), '[]')
                 FROM event_filter ef JOIN filter f ON f.id = ef.filter_id WHERE ef.event_id = e.id) AS filters
         FROM event e
         JOIN app_user u ON u.id = e.creator_id
         LEFT JOIN LATERAL (
             SELECT latitude, longitude FROM event_zone z WHERE z.event_id = e.id ORDER BY z.id LIMIT 1
         ) fz ON true
         WHERE ($1::text IS NULL OR EXISTS (
                  SELECT 1 FROM event_filter ef JOIN filter f ON f.id = ef.filter_id
                  WHERE ef.event_id = e.id AND f.name = $1))
           AND ($2::bigint IS NULL OR e.id > $2)
           AND ($3::timestamptz IS NULL OR
                (SELECT MIN(starts_at) FROM event_schedule_item s WHERE s.event_id = e.id) >= $3)
           AND ($4::timestamptz IS NULL OR
                (SELECT MIN(starts_at) FROM event_schedule_item s WHERE s.event_id = e.id) <= $4)
           AND ($5::text IS NULL OR LOWER(e.name) LIKE '%' || LOWER($5) || '%')
           AND ($6::int IS NULL OR (SELECT COUNT(*) FROM event_member m WHERE m.event_id = e.id) >= $6)
           AND ($7::int IS NULL OR (SELECT COUNT(*) FROM event_member m WHERE m.event_id = e.id) <= $7)
           AND ($8::boolean IS NULL OR EXISTS (SELECT 1 FROM event_zone z WHERE z.event_id = e.id) = $8)
           AND ($9::double precision IS NULL OR $10::double precision IS NULL OR $11::double precision IS NULL OR (
                fz.latitude IS NOT NULL AND
                6371 * acos(LEAST(1, GREATEST(-1,
                    cos(radians($9::double precision)) * cos(radians(fz.latitude)) * cos(radians(fz.longitude) - radians($10::double precision))
                    + sin(radians($9::double precision)) * sin(radians(fz.latitude))
                ))) <= $11::double precision
           ))
         ORDER BY ${orderBy}
         LIMIT $12`,
        [filter, after, from, to, name, minMembers, maxMembers, hasMap, lat, lng, radiusKm, limit + 1]
    );

    const items = rows.slice(0, limit).map((e) => ({ ...e, timing: timing(e.starts_at, e.ends_at) }));
    return { items, hasMore: rows.length > limit };
}));

// GET /events/bounds -- крайние значения для подсказок в фильтрах (мин/макс время, мин/макс участников).
router.get('/events/bounds', route(async () => {
    const row = await one(
        `SELECT
            (SELECT MIN(starts_at) FROM event_schedule_item) AS min_starts_at,
            (SELECT MAX(starts_at) FROM event_schedule_item) AS max_starts_at,
            (SELECT MIN(cnt) FROM (SELECT COUNT(*) cnt FROM event_member GROUP BY event_id) t) AS min_members,
            (SELECT MAX(cnt) FROM (SELECT COUNT(*) cnt FROM event_member GROUP BY event_id) t) AS max_members`
    );
    return {
        min_starts_at: row.min_starts_at,
        max_starts_at: row.max_starts_at,
        min_members: row.min_members ?? 0,
        max_members: row.max_members ?? 0,
    };
}));

// GET /events/:id -- карточка (+ моя роль/валидация, статус по времени).
router.get('/events/:id', route(async (req) => {
    const e = await one(
        `SELECT e.*,
                u.username AS creator_username,
                (SELECT COUNT(*) FROM event_member m WHERE m.event_id = e.id) AS members_count,
                (SELECT MIN(starts_at) FROM event_schedule_item s WHERE s.event_id = e.id) AS starts_at,
                (SELECT MAX(ends_at)   FROM event_schedule_item s WHERE s.event_id = e.id) AS ends_at,
                (SELECT role FROM event_member m WHERE m.event_id = e.id AND m.user_id = $2) AS my_role,
                (SELECT validated_at IS NOT NULL FROM event_member m WHERE m.event_id = e.id AND m.user_id = $2) AS my_validated,
                (SELECT COALESCE(json_agg(json_build_object(
                            'name', f.name, 'color_fg', f.color_fg, 'color_bg', f.color_bg
                        ) ORDER BY f.id), '[]')
                 FROM event_filter ef JOIN filter f ON f.id = ef.filter_id WHERE ef.event_id = e.id) AS filters
         FROM event e
         JOIN app_user u ON u.id = e.creator_id
         WHERE e.id = $1`,
        [req.params.id, req.user.id]
    );
    if (!e) throw new HttpError(404, 'event not found');
    e.timing = timing(e.starts_at, e.ends_at);
    return e;
}));

// GET /events/:id/goals -- список целей мероприятия.
router.get('/events/:id/goals', route(async (req) =>
    query('SELECT id, text, position FROM event_goal WHERE event_id = $1 ORDER BY position', [req.params.id])));

// GET /events/:id/schedule -- расписание мероприятия (пункты программы).
router.get('/events/:id/schedule', route(async (req) =>
    query('SELECT id, title, starts_at, ends_at, position FROM event_schedule_item WHERE event_id = $1 ORDER BY starts_at', [req.params.id])));

// GET /events/:id/zones -- точки/зоны мероприятия на карте.
router.get('/events/:id/zones', route(async (req) =>
    query(`SELECT id, schedule_item_id, latitude, longitude, label, is_zone, radius_m, color_fill, color_stroke
           FROM event_zone WHERE event_id = $1`, [req.params.id])));

async function insertNestedEntities(client, eventId, data) {
    if (data.filters?.length) {
        await client.query(
            `INSERT INTO event_filter (event_id, filter_id)
             SELECT $1, f.id FROM filter f WHERE f.scope = 'event' AND f.name = ANY($2::text[])`,
            [eventId, data.filters]
        );
    }

    const scheduleIds = [];
    for (let i = 0; i < data.schedule.length; ++i) {
        const s = data.schedule[i];
        const { rows: [row] } = await client.query(
            `INSERT INTO event_schedule_item (event_id, title, starts_at, ends_at, position)
             VALUES ($1, $2, $3, $4, $5) RETURNING id`,
            [eventId, s.title, s.starts_at, s.ends_at ?? null, i]
        );
        scheduleIds.push(row.id);
    }

    for (let i = 0; i < (data.goals?.length ?? 0); ++i) {
        await client.query('INSERT INTO event_goal (event_id, text, position) VALUES ($1, $2, $3)',
            [eventId, data.goals[i], i]);
    }

    for (const z of data.zones ?? []) {
        const schedItem = z.schedule_index != null ? scheduleIds[z.schedule_index] ?? null : null;
        await client.query(
            `INSERT INTO event_zone (event_id, schedule_item_id, latitude, longitude, label, is_zone, radius_m, color_fill, color_stroke)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
            [eventId, schedItem, z.latitude, z.longitude, z.label ?? null, z.is_zone ?? false,
             z.radius_m ?? null, z.color_fill ?? null, z.color_stroke ?? null]
        );
    }
}

// POST /events -- создать (нужно 50 очков опыта). Создатель = 'creator' и сразу validated.
router.post('/events', upload.single('image'), route(async (req) => {
    const data = validate(createSchema, parseBody(req.body));
    if (req.user.experience < 50) {
        throw new HttpError(403, 'Получите 50 очков опыта, чтобы создавать мероприятия');
    }
    const imageKey = await uploadImage(req.file, 'events');

    return tx(async (client) => {
        const { rows: [ev] } = await client.query(
            `INSERT INTO event (name, about, creator_id, start_condition, start_threshold, image_key)
             VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
            [data.name, data.about ?? null, req.user.id, data.start_condition, data.start_threshold ?? null, imageKey]
        );
        await client.query(
            `INSERT INTO event_member (event_id, user_id, role, validated_at) VALUES ($1, $2, 'creator', now())`,
            [ev.id, req.user.id]
        );
        await insertNestedEntities(client, ev.id, data);
        return ev;
    });
}));

// PATCH /events/:id -- редактировать (только создатель, только пока мероприятие не началось).
router.patch('/events/:id', upload.single('image'), route(async (req) => {
    const eventId = req.params.id;
    const ev = await one(
        `SELECT e.creator_id,
                (SELECT MIN(starts_at) FROM event_schedule_item s WHERE s.event_id = e.id) AS starts_at,
                (SELECT MAX(ends_at)   FROM event_schedule_item s WHERE s.event_id = e.id) AS ends_at
         FROM event e WHERE e.id = $1`,
        [eventId]
    );
    if (!ev) throw new HttpError(404, 'event not found');
    if (ev.creator_id !== req.user.id) throw new HttpError(403, 'only creator can edit');
    if (timing(ev.starts_at, ev.ends_at) !== 0) {
        throw new HttpError(409, 'cannot edit an event that has already started or finished');
    }

    const data = validate(createSchema, parseBody(req.body));
    const imageKey = req.file ? await uploadImage(req.file, 'events') : undefined;

    return tx(async (client) => {
        const { rows: [updated] } = await client.query(
            `UPDATE event SET name = $2, about = $3, start_condition = $4, start_threshold = $5,
                    image_key = COALESCE($6, image_key)
             WHERE id = $1 RETURNING *`,
            [eventId, data.name, data.about ?? null, data.start_condition, data.start_threshold ?? null, imageKey ?? null]
        );
        await client.query('DELETE FROM event_filter WHERE event_id = $1', [eventId]);
        await client.query('DELETE FROM event_schedule_item WHERE event_id = $1', [eventId]);
        await client.query('DELETE FROM event_goal WHERE event_id = $1', [eventId]);
        await client.query('DELETE FROM event_zone WHERE event_id = $1', [eventId]);
        await insertNestedEntities(client, eventId, data);
        return updated;
    });
}));

// DELETE /events/:id -- удалить (только создатель). FK каскадом снесёт всё вложенное.
router.delete('/events/:id', route(async (req) => {
    const ev = await one('SELECT creator_id FROM event WHERE id = $1', [req.params.id]);
    if (!ev) throw new HttpError(404, 'event not found');
    if (ev.creator_id !== req.user.id) throw new HttpError(403, 'only creator can delete');
    await one('DELETE FROM event WHERE id = $1 RETURNING id', [req.params.id]);
    return { deleted: true };
}));

module.exports = router;
