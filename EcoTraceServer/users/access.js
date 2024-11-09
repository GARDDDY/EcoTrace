const { one } = require('../core/db');

async function areFriends(a, b) {
    if (String(a) === String(b)) return true;
    const row = await one(
        `SELECT 1 FROM friendship
         WHERE status = 'accepted'
           AND ((requester_id = $1 AND addressee_id = $2)
             OR (requester_id = $2 AND addressee_id = $1))`,
        [a, b]
    );
    return !!row;
}

function canSee(visibility, isOwner, isFriend) {
    if (isOwner) return true;
    if (visibility === 'everyone') return true;
    if (visibility === 'friends') return isFriend;
    return false;
}

module.exports = { areFriends, canSee };
