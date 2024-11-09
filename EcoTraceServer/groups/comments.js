const express = require('express');
const multer = require('multer');
const { route, HttpError } = require('../core/route');
const { query, one } = require('../core/db');
const { uploadImage } = require('../core/upload');
const { publish } = require('../core/realtime');
const { requireMember, assertCanModerate } = require('./access');

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });

function postWithGroup(postId) {
    return one(
        `SELECT p.id, p.group_id, g.type AS group_type
         FROM post p JOIN "group" g ON g.id = p.group_id
         WHERE p.id = $1`,
        [postId]
    );
}

// GET /posts/:postId/comments -- список комментов. ?after=<id> -- только новые.
router.get('/posts/:postId/comments', route(async (req) => {
    const after = req.query.after || null;
    const post = await postWithGroup(req.params.postId);
    if (!post) throw new HttpError(404, 'post not found');
    if (post.group_type === 'closed') await requireMember(post.group_id, req.user.id);

    const rows = await query(
        `SELECT c.*,
                u.username AS author_username,
                gm.role    AS author_role
         FROM comment c
         JOIN app_user u ON u.id = c.author_id
         LEFT JOIN group_member gm ON gm.group_id = $2 AND gm.user_id = c.author_id
         WHERE c.post_id = $1 AND ($3::bigint IS NULL OR c.id > $3)
         ORDER BY c.id
         LIMIT 20`,
        [req.params.postId, post.group_id, after]
    );
    return { items: rows, hasMore: rows.length === 20 };
}));

// POST /posts/:postId/comments -- добавить коммент (любой участник группы).
router.post('/posts/:postId/comments', upload.single('image'), route(async (req) => {
    const post = await postWithGroup(req.params.postId);
    if (!post) throw new HttpError(404, 'post not found');
    const m = await requireMember(post.group_id, req.user.id);

    const imageKey = await uploadImage(req.file, 'comments');
    const comment = await one(
        `INSERT INTO comment (post_id, author_id, text, image_key)
         VALUES ($1, $2, $3, $4) RETURNING *`,
        [post.id, req.user.id, req.body.text ?? null, imageKey]
    );

    publish(`post:${post.id}:comments`, 'comment:new', {
        ...comment, author_username: req.user.username, author_role: m.role,
    });
    return comment;
}));

// DELETE /comments/:commentId -- удалить (автор, либо помощник+ выше автора по рангу).
router.delete('/comments/:commentId', route(async (req) => {
    const c = await one(
        `SELECT c.author_id, c.post_id, p.group_id
         FROM comment c JOIN post p ON p.id = c.post_id
         WHERE c.id = $1`,
        [req.params.commentId]
    );
    if (!c) throw new HttpError(404, 'comment not found');

    await assertCanModerate(c.group_id, req.user.id, c.author_id);
    await one('DELETE FROM comment WHERE id = $1 RETURNING id', [req.params.commentId]);

    publish(`post:${c.post_id}:comments`, 'comment:deleted', { id: Number(req.params.commentId) });
    return { deleted: true };
}));

module.exports = router;
