const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

const TEST_DB = 'ecotrace_test';

const ADMIN_DB_URL = process.env.DATABASE_URL.replace('ecotrace_test', 'postgres');
const TEST_DB_URL = process.env.DATABASE_URL;

module.exports = async () => {
  const admin = new Client({ connectionString: ADMIN_DB_URL });
  await admin.connect();
  await admin.query(`
    SELECT pg_terminate_backend(pid)
    FROM pg_stat_activity
    WHERE datname = '${TEST_DB}';
  `);

  await admin.query(`DROP DATABASE IF EXISTS ${TEST_DB}`);
  await admin.query(`CREATE DATABASE ${TEST_DB}`);

  await admin.end();

  const test = new Client({ connectionString: TEST_DB_URL });
  await test.connect();

  const sql = fs.readFileSync(
    path.join(__dirname, '..', '..', 'init.sql'),
    'utf8'
  );

  await test.query(sql);
  await test.end();
};