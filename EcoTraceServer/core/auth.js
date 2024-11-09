const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { one } = require('./db');

const SESSION_TTL_DAYS = 30;

async function authenticate(token) {
    if (!token) return null;
    const user = await one(
        `SELECT u.*
         FROM session s
         JOIN app_user u ON u.id = s.user_id
         WHERE s.token = $1 AND s.expires_at > now()`,
        [token]
    );
    if (user) delete user.password_hash;
    return user;
}

async function createSession(userId) {
    const token = crypto.randomBytes(32).toString('hex');
    await one(
        `INSERT INTO session (user_id, token, expires_at)
         VALUES ($1, $2, now() + make_interval(days => $3))`,
        [userId, token, SESSION_TTL_DAYS]
    );
    return token;
}

async function destroySession(token) {
    if (!token) return;
    await one('DELETE FROM session WHERE token = $1 RETURNING id', [token]);
}

async function destroyUserSessions(userId) {
    await one('DELETE FROM session WHERE user_id = $1 RETURNING id', [userId]);
}

function bearerToken(req) {
    return (req.get('Authorization') || '').replace(/^Bearer\s+/i, '');
}

function hashPassword(plain) {
    return bcrypt.hash(plain, 10);
}

function verifyPassword(plain, hash) {
    return bcrypt.compare(plain, hash);
}

module.exports = {
    authenticate,
    createSession,
    destroySession,
    destroyUserSessions,
    bearerToken,
    hashPassword,
    verifyPassword,
};
