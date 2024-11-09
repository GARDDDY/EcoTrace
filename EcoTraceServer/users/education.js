const express = require('express');
const { z } = require('zod');
const { route, HttpError } = require('../core/route');
const { validate } = require('../core/validate');
const { query, one, tx } = require('../core/db');

const router = express.Router();

const EDU_REWARD = 5;

// GET /education -- список пройденных модулей пользователя.
router.get('/education', route(async (req) => {
    return query(
        `SELECT module_id, correct_answers, total_questions, completed_at
         FROM education_completion WHERE user_id = $1
         ORDER BY module_id`,
        [req.user.id]
    );
}));

// GET /education/:moduleId -- пройден ли модуль.
router.get('/education/:moduleId', route(async (req) => {
    const row = await one(
        'SELECT 1 FROM education_completion WHERE user_id = $1 AND module_id = $2',
        [req.user.id, req.params.moduleId]
    );
    return { completed: !!row };
}));

// POST /education/:moduleId/complete -- отметить пройденным и начислить опыт (идемпотентно).
const completeSchema = z.object({
    correct: z.number().int().min(0).optional(),
    total: z.number().int().min(0).optional(),
});
router.post('/education/:moduleId/complete', route(async (req) => {
    const { correct = 0, total = 0 } = validate(completeSchema, req.body || {});
    const { moduleId } = req.params;

    if (!(await one('SELECT 1 FROM education_module WHERE id = $1', [moduleId]))) {
        throw new HttpError(404, 'module not found');
    }

    return tx(async (client) => {
        const { rowCount } = await client.query(
            `INSERT INTO education_completion (user_id, module_id, correct_answers, total_questions)
             VALUES ($1, $2, $3, $4) ON CONFLICT (user_id, module_id) DO NOTHING`,
            [req.user.id, moduleId, correct, total]
        );
        if (rowCount === 0) return { awarded: false, reason: 'already completed' };

        await client.query(
            `INSERT INTO experience_ledger (user_id, amount, reason, ref_id) VALUES ($1, $2, 'education', $3)`,
            [req.user.id, EDU_REWARD, moduleId]
        );
        await client.query('UPDATE app_user SET experience = experience + $1 WHERE id = $2',
            [EDU_REWARD, req.user.id]);
        return { awarded: true, amount: EDU_REWARD };
    });
}));

module.exports = router;
