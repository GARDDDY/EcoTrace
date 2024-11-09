const { Pool, types } = require('pg');
const logger = require('../logger');

types.setTypeParser(20, (v) => (v === null ? null : parseInt(v, 10)));

const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.DATABASE_SSL === 'false' ? false : { rejectUnauthorized: false },
    max: 10,
});

pool.on('error', (err) => logger.error('Unexpected PG pool error', { err: err.message }));

async function query(text, params = []) {
    const res = await pool.query(text, params);
    return res.rows;
}

async function one(text, params = []) {
    const rows = await query(text, params);
    return rows[0] ?? null;
}

async function tx(fn) {
    const client = await pool.connect();
    try {
        await client.query('BEGIN');
        const result = await fn(client);
        await client.query('COMMIT');
        return result;
    } catch (e) {
        await client.query('ROLLBACK');
        throw e;
    } finally {
        client.release();
    }
}

async function ping() {
    await pool.query('SELECT 1');
    logger.info('Connected to PostgreSQL');
}

module.exports = { pool, query, one, tx, ping };
