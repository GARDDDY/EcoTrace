// Env для тестов: отдельная БД + локальное хранилище (выполняется до загрузки app).
process.env.NODE_ENV = 'test';
process.env.DATABASE_URL = process.env.TEST_DATABASE_URL
    || 'postgres://postgres:postgres@localhost:5432/ecotrace_test';
process.env.DATABASE_SSL = 'false';
