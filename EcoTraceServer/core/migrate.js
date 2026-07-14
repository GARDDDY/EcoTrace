const fs = require('fs');
const path = require('path');
const { pool } = require('./db');
const logger = require('../logger');

const MIGRATIONS_DIR = path.join(__dirname, '..', 'migrations');

async function migrate() {
    await pool.query(`
        CREATE TABLE IF NOT EXISTS schema_migrations (
            name       TEXT PRIMARY KEY,
            applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
        )
    `);

    const files = fs.readdirSync(MIGRATIONS_DIR)
        .filter((f) => f.endsWith('.sql'))
        .sort();

    const { rows: applied } = await pool.query('SELECT name FROM schema_migrations');
    const appliedSet = new Set(applied.map((r) => r.name));
    const pending = files.filter((f) => !appliedSet.has(f));

    if (pending.length === 0) {
        logger.info('No pending migrations');
        return;
    }

    for (const file of pending) {
        const sql = fs.readFileSync(path.join(MIGRATIONS_DIR, file), 'utf8');
        const client = await pool.connect();
        try {
            await client.query('BEGIN');
            await client.query(sql);
            await client.query('INSERT INTO schema_migrations (name) VALUES ($1)', [file]);
            await client.query('COMMIT');
            logger.info(`Applied migration ${file}`);
        } catch (e) {
            await client.query('ROLLBACK');
            throw e;
        } finally {
            client.release();
        }
    }
}

module.exports = { migrate };
