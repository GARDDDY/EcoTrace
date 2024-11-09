const { one } = require('../core/db');
const { HttpError } = require('../core/route');

const ROLE_RANK = { participant: 0, assistant: 1, manager: 2, creator: 3 };

function membership(groupId, userId) {
    return one(
        'SELECT role FROM group_member WHERE group_id = $1 AND user_id = $2',
        [groupId, userId]
    );
}

async function requireMember(groupId, userId) {
    const m = await membership(groupId, userId);
    if (!m) throw new HttpError(403, 'not a group member');
    return m;
}

async function requireRole(groupId, userId, minRole) {
    const m = await requireMember(groupId, userId);
    if (ROLE_RANK[m.role] < ROLE_RANK[minRole]) {
        throw new HttpError(403, `requires role >= ${minRole}`);
    }
    return m;
}

async function assertCanModerate(groupId, userId, authorId) {
    if (authorId === userId) return;
    const me = await requireRole(groupId, userId, 'assistant');
    const author = await membership(groupId, authorId);
    const authorRank = author ? ROLE_RANK[author.role] : -1;
    if (ROLE_RANK[me.role] <= authorRank) {
        throw new HttpError(403, 'insufficient role to moderate');
    }
}

module.exports = { ROLE_RANK, membership, requireMember, requireRole, assertCanModerate };
