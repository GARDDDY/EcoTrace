const express = require('express');
const { z } = require('zod');
const { route, HttpError } = require('../core/route');
const { validate } = require('../core/validate');
const { one } = require('../core/db');
const {
    createSession, destroySession, destroyUserSessions, bearerToken,
    hashPassword, verifyPassword,
} = require('../core/auth');
const { sendResetCode } = require('./email');

const router = express.Router();

const USER_PUBLIC = `id, username, email, fullname, gender, about_me, country_code,
    experience, avatar_key, name_visibility, country_visibility,
    friends_visibility, groups_visibility, created_at`;

const registerSchema = z.object({
    username: z.string().min(3).max(32),
    email: z.string().email(),
    password: z.string().min(6),
    fullname: z.string().max(64).optional(),
    gender: z.enum(['unknown', 'male', 'female']).optional(),
    about_me: z.string().max(500).optional(),
});

const loginSchema = z.object({
    login: z.string().min(1),
    password: z.string().min(1),
});

const emailSchema = z.object({ email: z.string().email() });
const codeSchema = z.object({ email: z.string().email(), code: z.string().regex(/^\d{6}$/) });
const changePwSchema = z.object({
    email: z.string().email(),
    code: z.string().regex(/^\d{6}$/),
    password: z.string().min(6),
});

async function resolveCountry(ip) {
    try {
        const r = await fetch(`https://ipinfo.io/${ip}/json`);
        const data = await r.json();
        return data.country || 'RU';
    } catch {
        return 'RU';
    }
}

// POST /register {username,email,password,fullname?,gender?,about_me?} -- регистрация нового пользователя.
router.post('/register', route(async (req) => {
    const data = validate(registerSchema, req.body);

    const taken = await one(
        'SELECT 1 FROM app_user WHERE username = $1 OR email = $2',
        [data.username, data.email]
    );
    if (taken) throw new HttpError(409, 'username or email already taken');

    const [countryCode, passwordHash] = await Promise.all([
        resolveCountry(req.ip),
        hashPassword(data.password),
    ]);

    const user = await one(
        `INSERT INTO app_user (username, email, password_hash, fullname, gender, about_me, country_code)
         VALUES ($1, $2, $3, $4, $5, $6, $7)
         RETURNING ${USER_PUBLIC}`,
        [data.username, data.email, passwordHash, data.fullname ?? null,
         data.gender ?? 'unknown', data.about_me ?? null, countryCode]
    );

    const token = await createSession(user.id);
    return { user, token };
}, { auth: false }));

// POST /login {login,password} -- вход по email или username.
router.post('/login', route(async (req) => {
    const { login, password } = validate(loginSchema, req.body);

    const user = await one(
        'SELECT * FROM app_user WHERE email = $1 OR username = $1',
        [login]
    );
    if (!user || !(await verifyPassword(password, user.password_hash))) {
        throw new HttpError(401, 'invalid credentials');
    }
    delete user.password_hash;

    const token = await createSession(user.id);
    return { user, token };
}, { auth: false }));

// POST /logout -- разлогинить текущую сессию (по токену).
router.post('/logout', route(async (req) => {
    await destroySession(bearerToken(req));
    return { loggedOut: true };
}));

// GET /me -- текущий пользователь (проверка токена).
router.get('/me', route(async (req) => req.user));

// POST /password/send-code {email} -- сброс пароля 1/3: отправить код на почту.
router.post('/password/send-code', route(async (req) => {
    const { email } = validate(emailSchema, req.body);

    const user = await one('SELECT username, fullname FROM app_user WHERE email = $1', [email]);
    if (!user) return { sent: false };

    const code = Math.floor(100000 + Math.random() * 900000);
    await one(
        `INSERT INTO auth_code (email, type, code, expires_at, applied_at)
         VALUES ($1, 'password_reset', $2, now() + interval '5 minutes', NULL)
         ON CONFLICT (email, type)
         DO UPDATE SET code = EXCLUDED.code, expires_at = EXCLUDED.expires_at, applied_at = NULL`,
        [email, code]
    );
    await sendResetCode(email, user.fullname ?? user.username, code);
    return { sent: true };
}, { auth: false }));

// POST /password/verify-code {email,code} -- сброс пароля 2/3: проверить код.
router.post('/password/verify-code', route(async (req) => {
    const { email, code } = validate(codeSchema, req.body);

    const row = await one(
        `UPDATE auth_code SET applied_at = now()
         WHERE email = $1 AND type = 'password_reset' AND code = $2
           AND expires_at > now() AND applied_at IS NULL
         RETURNING email`,
        [email, Number(code)]
    );
    return { valid: !!row };
}, { auth: false }));

// POST /password/change {email,code,password} -- сброс пароля 3/3: задать новый пароль.
router.post('/password/change', route(async (req) => {
    const { email, code, password } = validate(changePwSchema, req.body);

    const ok = await one(
        `SELECT 1 FROM auth_code
         WHERE email = $1 AND type = 'password_reset' AND code = $2
           AND expires_at > now() AND applied_at IS NOT NULL`,
        [email, Number(code)]
    );
    if (!ok) throw new HttpError(400, 'invalid or unverified code');

    const hash = await hashPassword(password);
    const user = await one(
        'UPDATE app_user SET password_hash = $1 WHERE email = $2 RETURNING id',
        [hash, email]
    );
    if (!user) throw new HttpError(404, 'user not found');

    await one(`DELETE FROM auth_code WHERE email = $1 AND type = 'password_reset' RETURNING email`, [email]);
    await destroyUserSessions(user.id);
    return { changed: true };
}, { auth: false }));

module.exports = router;
