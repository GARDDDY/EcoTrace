const express = require('express');
const multer = require('multer');
const { route, HttpError } = require('../core/route');
const { query, one, tx } = require('../core/db');
const { uploadImage } = require('../core/upload');
const { publish } = require('../core/realtime');
const { requireMember, requireRole, assertCanModerate } = require('./access');

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });

// GET /groups/:groupId/posts?before=&after= -- лента постов.
router.get('/groups/:groupId/posts', route(async (req) => {
    const { groupId } = req.params;
    const after = req.query.after || null;
    const before = req.query.before || null;
    const limit = 5;

    const group = await one('SELECT type FROM "group" WHERE id = $1', [groupId]);
    if (!group) throw new HttpError(404, 'group not found');
    if (group.type === 'closed') await requireMember(groupId, req.user.id);

    const rows = await query(
        `SELECT p.*,
                u.username   AS author_username,
                u.avatar_key AS author_avatar_key,
                gm.role      AS author_role,
                (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id) AS comments_count
         FROM post p
         JOIN app_user u ON u.id = p.author_id
         LEFT JOIN group_member gm ON gm.group_id = p.group_id AND gm.user_id = p.author_id
         WHERE p.group_id = $1
           AND ($2::bigint IS NULL OR p.id > $2)
           AND ($3::bigint IS NULL OR p.id < $3)
         ORDER BY p.id ${after !== null ? 'ASC' : 'DESC'}
         LIMIT $4`,
        [groupId, after, before, limit + 1]
    );
    return { items: rows.slice(0, limit), hasMore: rows.length > limit };
}));

// GET /groups/:groupId/posts/pinned -- закреплённый пост группы (или null, если такого нет).
router.get('/groups/:groupId/posts/pinned', route(async (req) => {
    const { groupId } = req.params;
    const post = await one(
        `SELECT p.*,
                u.username   AS author_username,
                u.avatar_key AS author_avatar_key,
                gm.role      AS author_role,
                (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id) AS comments_count
         FROM post p
         JOIN app_user u ON u.id = p.author_id
         LEFT JOIN group_member gm ON gm.group_id = p.group_id AND gm.user_id = p.author_id
         WHERE p.group_id = $1 AND p.pinned_at IS NOT NULL`,
        [groupId]
    );
    return post || null;
}));

// POST /groups/:groupId/posts -- создать пост (любой участник).
router.post('/groups/:groupId/posts', upload.single('image'), route(async (req) => {
    const { groupId } = req.params;
    const m = await requireMember(groupId, req.user.id);

    const imageKey = await uploadImage(req.file, 'posts');
    const post = await one(
        `INSERT INTO post (group_id, author_id, text, image_key)
         VALUES ($1, $2, $3, $4) RETURNING *`,
        [groupId, req.user.id, req.body.text ?? null, imageKey]
    );

    publish(`group:${groupId}:posts`, 'post:new', {
        ...post,
        author_username: req.user.username,
        author_avatar_key: req.user.avatar_key,
        author_role: m.role,
        comments_count: 0,
    });
    return post;
}));

// DELETE /posts/:postId -- удалить (автор, либо помощник+ выше автора по рангу).
router.delete('/posts/:postId', route(async (req) => {
    const post = await one('SELECT group_id, author_id FROM post WHERE id = $1', [req.params.postId]);
    if (!post) throw new HttpError(404, 'post not found');

    await assertCanModerate(post.group_id, req.user.id, post.author_id);
    await one('DELETE FROM post WHERE id = $1 RETURNING id', [req.params.postId]);

    publish(`group:${post.group_id}:posts`, 'post:deleted', { id: Number(req.params.postId) });
    return { deleted: true };
}));

// POST /posts/:postId/pin -- закрепить (помощник+).
router.post('/posts/:postId/pin', route(async (req) => {
    const post = await one('SELECT group_id FROM post WHERE id = $1', [req.params.postId]);
    if (!post) throw new HttpError(404, 'post not found');
    await requireRole(post.group_id, req.user.id, 'assistant');

    return tx(async (client) => {
        await client.query(
            'UPDATE post SET pinned_at = NULL WHERE group_id = $1 AND pinned_at IS NOT NULL',
            [post.group_id]
        );
        const { rows: [updated] } = await client.query(
            'UPDATE post SET pinned_at = now() WHERE id = $1 RETURNING *',
            [req.params.postId]
        );
        publish(`group:${post.group_id}:posts`, 'post:pinned', { id: updated.id, pinnedAt: updated.pinned_at });
        return updated;
    });
}));

// DELETE /posts/:postId/pin -- открепить (помощник+).
router.delete('/posts/:postId/pin', route(async (req) => {
    const post = await one('SELECT group_id FROM post WHERE id = $1', [req.params.postId]);
    if (!post) throw new HttpError(404, 'post not found');
    await requireRole(post.group_id, req.user.id, 'assistant');

    await one('UPDATE post SET pinned_at = NULL WHERE id = $1 RETURNING id', [req.params.postId]);
    publish(`group:${post.group_id}:posts`, 'post:unpinned', { id: Number(req.params.postId) });
    return { unpinned: true };
}));

module.exports = router;
