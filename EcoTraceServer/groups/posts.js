const express = require('express');
const multer = require('multer');
const { route, HttpError } = require('../core/route');
const { query, one, tx } = require('../core/db');
const { uploadImages } = require('../core/upload');
const { publish } = require('../core/realtime');
const { requireMember, requireRole, assertCanModerate } = require('./access');

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });
const MAX_POST_IMAGES = 6;

const POST_SELECT = `
    SELECT p.*,
           u.username   AS author_username,
           u.avatar_key AS author_avatar_key,
           gm.role      AS author_role,
           (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id) AS comments_count,
           COALESCE(
               (SELECT json_agg(pi.image_key ORDER BY pi.position) FROM post_image pi WHERE pi.post_id = p.id),
               '[]'
           ) AS image_keys
    FROM post p
    JOIN app_user u ON u.id = p.author_id
    LEFT JOIN group_member gm ON gm.group_id = p.group_id AND gm.user_id = p.author_id
`;

router.get('/groups/:groupId/posts', route(async (req) => {
    const { groupId } = req.params;
    const after = req.query.after || null;
    const before = req.query.before || null;
    const limit = 5;

    const group = await one('SELECT type FROM "group" WHERE id = $1', [groupId]);
    if (!group) throw new HttpError(404, 'group not found');
    if (group.type === 'closed') await requireMember(groupId, req.user.id);

    const rows = await query(
        `${POST_SELECT}
         WHERE p.group_id = $1
           AND ($2::bigint IS NULL OR p.id > $2)
           AND ($3::bigint IS NULL OR p.id < $3)
         ORDER BY p.id ${after !== null ? 'ASC' : 'DESC'}
         LIMIT $4`,
        [groupId, after, before, limit + 1]
    );
    return { items: rows.slice(0, limit), has_more: rows.length > limit };
}));

router.get('/groups/:groupId/posts/pinned', route(async (req) => {
    const { groupId } = req.params;
    const post = await one(`${POST_SELECT} WHERE p.group_id = $1 AND p.pinned_at IS NOT NULL`, [groupId]);
    return post || null;
}));

router.get('/posts/:postId', route(async (req) => {
    const post = await one(`${POST_SELECT} WHERE p.id = $1`, [req.params.postId]);
    if (!post) throw new HttpError(404, 'post not found');
    const group = await one('SELECT type FROM "group" WHERE id = $1', [post.group_id]);
    if (group?.type === 'closed') await requireMember(post.group_id, req.user.id);
    return post;
}));

router.post('/groups/:groupId/posts', upload.array('images', MAX_POST_IMAGES), route(async (req) => {
    const { groupId } = req.params;
    await requireMember(groupId, req.user.id);

    const imageKeys = await uploadImages(req.files, 'posts');

    const post = await tx(async (client) => {
        const { rows: [inserted] } = await client.query(
            `INSERT INTO post (group_id, author_id, text) VALUES ($1, $2, $3) RETURNING id`,
            [groupId, req.user.id, req.body.text ?? null]
        );
        if (imageKeys.length > 0) {
            const values = imageKeys.map((_, i) => `($1, $${i * 2 + 2}, $${i * 2 + 3})`).join(', ');
            const params = [inserted.id, ...imageKeys.flatMap((key, i) => [key, i])];
            await client.query(`INSERT INTO post_image (post_id, image_key, position) VALUES ${values}`, params);
        }
        const { rows: [full] } = await client.query(`${POST_SELECT} WHERE p.id = $1`, [inserted.id]);
        return full;
    });

    publish(`group:${groupId}:posts`, 'post:new', post);
    return post;
}));

router.delete('/posts/:postId', route(async (req) => {
    const post = await one('SELECT group_id, author_id FROM post WHERE id = $1', [req.params.postId]);
    if (!post) throw new HttpError(404, 'post not found');

    await assertCanModerate(post.group_id, req.user.id, post.author_id);
    await one('DELETE FROM post WHERE id = $1 RETURNING id', [req.params.postId]);

    publish(`group:${post.group_id}:posts`, 'post:deleted', { id: Number(req.params.postId) });
    return { deleted: true };
}));

router.post('/posts/:postId/pin', route(async (req) => {
    const post = await one('SELECT group_id FROM post WHERE id = $1', [req.params.postId]);
    if (!post) throw new HttpError(404, 'post not found');
    await requireRole(post.group_id, req.user.id, 'assistant');

    const updated = await tx(async (client) => {
        await client.query(
            'UPDATE post SET pinned_at = NULL WHERE group_id = $1 AND pinned_at IS NOT NULL',
            [post.group_id]
        );
        await client.query('UPDATE post SET pinned_at = now() WHERE id = $1', [req.params.postId]);
        const { rows: [full] } = await client.query(`${POST_SELECT} WHERE p.id = $1`, [req.params.postId]);
        return full;
    });
    publish(`group:${post.group_id}:posts`, 'post:pinned', { id: updated.id, pinnedAt: updated.pinned_at });
    return updated;
}));

router.delete('/posts/:postId/pin', route(async (req) => {
    const post = await one('SELECT group_id FROM post WHERE id = $1', [req.params.postId]);
    if (!post) throw new HttpError(404, 'post not found');
    await requireRole(post.group_id, req.user.id, 'assistant');

    await one('UPDATE post SET pinned_at = NULL WHERE id = $1 RETURNING id', [req.params.postId]);
    publish(`group:${post.group_id}:posts`, 'post:unpinned', { id: Number(req.params.postId) });
    return { unpinned: true };
}));

module.exports = router;
