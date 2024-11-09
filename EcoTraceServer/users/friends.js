const express = require('express');
const { route, HttpError } = require('../core/route');
const { query, one } = require('../core/db');
const { areFriends, canSee } = require('./access');

const router = express.Router();

// GET /users/:id/friends?sort=rating&after=<id> -- список друзей (с учётом friends_visibility).
router.get('/users/:id/friends', route(async (req) => {
    const targetId = req.params.id === 'me' ? req.user.id : req.params.id;
    const after = req.query.after || null;
    const sort = req.query.sort || null;
    const limit = 20;

    const target = await one('SELECT id, friends_visibility FROM app_user WHERE id = $1', [targetId]);
    if (!target) throw new HttpError(404, 'user not found');

    const isOwner = String(target.id) === String(req.user.id);
    const isFriend = isOwner ? true : await areFriends(req.user.id, target.id);
    if (!canSee(target.friends_visibility, isOwner, isFriend)) {
        return { items: [], hasMore: false, hidden: true };
    }

    const orderBy = sort === 'rating' ? 'u.experience DESC, u.id' : 'u.id';

    const rows = await query(
        `SELECT u.id, u.username, u.avatar_key, u.experience
         FROM friendship f
         JOIN app_user u ON u.id = CASE WHEN f.requester_id = $1 THEN f.addressee_id ELSE f.requester_id END
         WHERE f.status = 'accepted' AND $1 IN (f.requester_id, f.addressee_id)
           AND ($2::bigint IS NULL OR u.id > $2)
         ORDER BY ${orderBy}
         LIMIT $3`,
        [target.id, after, limit + 1]
    );
    return { items: rows.slice(0, limit), hasMore: rows.length > limit };
}));

// GET /users/:id/friendship -- статус по отношению ко мне (code: 0 нет связи, 1 я отправил, 2 мне отправили, 3 друзья).
router.get('/users/:id/friendship', route(async (req) => {
    const otherId = req.params.id;
    if (String(otherId) === String(req.user.id)) return { code: 0, self: true };

    const f = await one(
        `SELECT requester_id, status FROM friendship
         WHERE (requester_id = $1 AND addressee_id = $2) OR (requester_id = $2 AND addressee_id = $1)`,
        [req.user.id, otherId]
    );
    if (!f) return { code: 0 };
    if (f.status === 'accepted') return { code: 3 };
    return { code: String(f.requester_id) === String(req.user.id) ? 1 : 2 };
}));

// POST /users/:id/friend -- отправить заявку или принять входящую.
router.post('/users/:id/friend', route(async (req) => {
    const otherId = req.params.id;
    if (String(otherId) === String(req.user.id)) throw new HttpError(400, 'cannot friend yourself');
    if (!(await one('SELECT 1 FROM app_user WHERE id = $1', [otherId]))) throw new HttpError(404, 'user not found');

    const f = await one(
        `SELECT id, requester_id, status FROM friendship
         WHERE (requester_id = $1 AND addressee_id = $2) OR (requester_id = $2 AND addressee_id = $1)`,
        [req.user.id, otherId]
    );

    if (!f) {
        await one(`INSERT INTO friendship (requester_id, addressee_id, status) VALUES ($1, $2, 'pending')`,
            [req.user.id, otherId]);
        return { code: 1 };
    }
    if (f.status === 'accepted') return { code: 3 };
    if (String(f.requester_id) === String(req.user.id)) return { code: 1 };

    await one(`UPDATE friendship SET status = 'accepted', responded_at = now() WHERE id = $1`, [f.id]);
    return { code: 3 };
}));

// DELETE /users/:id/friend -- удалить дружбу / отменить или отклонить заявку.
router.delete('/users/:id/friend', route(async (req) => {
    await one(
        `DELETE FROM friendship
         WHERE (requester_id = $1 AND addressee_id = $2) OR (requester_id = $2 AND addressee_id = $1)
         RETURNING id`,
        [req.user.id, req.params.id]
    );
    return { removed: true };
}));

module.exports = router;
