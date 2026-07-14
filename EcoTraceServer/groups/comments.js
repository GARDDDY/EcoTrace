const express = require('express');
const multer = require('multer');
const { route, HttpError } = require('../core/route');
const { query, one, tx } = require('../core/db');
const { uploadImages } = require('../core/upload');
const { publish } = require('../core/realtime');
const { requireMember, assertCanModerate } = require('./access');

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });
const MAX_COMMENT_IMAGES = 6;

const COMMENT_SELECT = `
    SELECT c.*,
           u.username   AS author_username,
           u.avatar_key AS author_avatar_key,
           gm.role      AS author_role,
           COALESCE(
               (SELECT json_agg(ci.image_key ORDER BY ci.position) FROM comment_image ci WHERE ci.comment_id = c.id),
               '[]'
           ) AS image_keys
    FROM comment c
    JOIN app_user u ON u.id = c.author_id
    LEFT JOIN group_member gm ON gm.group_id = $2 AND gm.user_id = c.author_id
`;

function postWithGroup(postId) {
    return one(
        `SELECT p.id, p.group_id, g.type AS group_type
         FROM post p JOIN "group" g ON g.id = p.group_id
         WHERE p.id = $1`,
        [postId]
    );
}

// GET /posts/:postId/comments?after -- список комментариев поста постранично (по 20, курсор after)
router.get('/posts/:postId/comments', route(async (req) => {
    const after = req.query.after || null;
    const post = await postWithGroup(req.params.postId);
    if (!post) throw new HttpError(404, 'post not found');
    if (post.group_type === 'closed') await requireMember(post.group_id, req.user.id);

    const rows = await query(
        `${COMMENT_SELECT}
         WHERE c.post_id = $1 AND ($3::bigint IS NULL OR c.id > $3)
         ORDER BY c.id
         LIMIT 20`,
        [req.params.postId, post.group_id, after]
    );
    return { items: rows, has_more: rows.length === 20 };
}));

// POST /posts/:postId/comments -- добавить комментарий к посту (текст + до 6 изображений)
router.post('/posts/:postId/comments', upload.array('images', MAX_COMMENT_IMAGES), route(async (req) => {
    const post = await postWithGroup(req.params.postId);
    if (!post) throw new HttpError(404, 'post not found');
    await requireMember(post.group_id, req.user.id);

    const imageKeys = await uploadImages(req.files, 'comments');

    const comment = await tx(async (client) => {
        const { rows: [inserted] } = await client.query(
            `INSERT INTO comment (post_id, author_id, text) VALUES ($1, $2, $3) RETURNING id`,
            [post.id, req.user.id, req.body.text ?? null]
        );
        if (imageKeys.length > 0) {
            const values = imageKeys.map((_, i) => `($1, $${i * 2 + 2}, $${i * 2 + 3})`).join(', ');
            const params = [inserted.id, ...imageKeys.flatMap((key, i) => [key, i])];
            await client.query(`INSERT INTO comment_image (comment_id, image_key, position) VALUES ${values}`, params);
        }
        const { rows: [full] } = await client.query(
            `${COMMENT_SELECT} WHERE c.id = $1`,
            [inserted.id, post.group_id]
        );
        return full;
    });

    publish(`post:${post.id}:comments`, 'comment:new', comment);
    return comment;
}));

// DELETE /comments/:commentId -- удалить комментарий (автор или модератор группы)
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
