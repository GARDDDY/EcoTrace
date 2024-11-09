// Общие помощники для тестов: чистый стол, регистрация, заголовок авторизации.
const request = require('supertest');
const app = require('../app');
const { pool } = require('../core/db');

let counter = 0;
const uniq = () => `${Date.now().toString(36)}${counter++}`;

// Очистить все пользовательские данные (справочники filter/calculator остаются).
async function reset() {
    await pool.query('TRUNCATE app_user RESTART IDENTITY CASCADE');
}

async function register(over = {}) {
    const u = uniq();
    const body = { username: `u${u}`, email: `${u}@e.com`, password: 'secret123', ...over };
    const res = await request(app).post('/register').send(body);
    return { res, token: res.body.data && res.body.data.token, user: res.body.data && res.body.data.user };
}

const authHeader = (token) => ({ Authorization: `Bearer ${token}` });

async function setExp(userId, exp) {
    await pool.query('UPDATE app_user SET experience = $1 WHERE id = $2', [exp, userId]);
}

module.exports = { app, request, pool, reset, register, authHeader, setExp };
