const { one } = require('../core/db');
const { HttpError } = require('../core/route');

const ROLE_RANK = { participant: 0, assistant: 1, creator: 2 };

function membership(eventId, userId) {
    return one(
        'SELECT role, validated_at FROM event_member WHERE event_id = $1 AND user_id = $2',
        [eventId, userId]
    );
}

async function requireMember(eventId, userId) {
    const m = await membership(eventId, userId);
    if (!m) throw new HttpError(403, 'not an event member');
    return m;
}

async function requireRole(eventId, userId, minRole) {
    const m = await requireMember(eventId, userId);
    if (ROLE_RANK[m.role] < ROLE_RANK[minRole]) {
        throw new HttpError(403, `requires role >= ${minRole}`);
    }
    return m;
}

function timing(startsAt, endsAt) {
    if (!startsAt) return 0;
    const now = Date.now();
    const s = new Date(startsAt).getTime();
    const e = endsAt ? new Date(endsAt).getTime() : s;
    if (now < s) return 0;
    if (now <= e) return 1;
    return 2;
}

module.exports = { ROLE_RANK, membership, requireMember, requireRole, timing };
