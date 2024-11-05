const request = require('supertest');
const express = require('express');
const router = require('../user/setEcoData');

const app = express();
app.use(express.json());
app.use(router);

jest.mock('../tech/oauth', () => ({
    checkOAuth2: jest.fn(),
}));

jest.mock("../server", () => ({
    users: { /* моки для соединения с БД */ },
    calculator: { /* моки для соединения с БД */ },
}));

const { checkOAuth2 } = require('../tech/oauth');

describe('POST /setEcoData', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should return 403 if not authenticated', async () => {
        checkOAuth2.mockResolvedValue(false);

        const response = await request(app)
            .post('/setEcoData')
            .query({ cuid: '1', oauth: 'invalid' })
            .send([{ question: 1, value: 10 }]);

        expect(response.status).toBe(403);
        expect(response.body).toEqual({ error: "You are not signed in! Not allowed" });
    });

    it('should process data correctly', async () => {
        checkOAuth2.mockResolvedValue(true);

        // Настройка мока для выполнения запросов к базе данных
        const mockUserId = '1';
        const mockData = [{ question: 1, value: 10 }];
        
        // Имитация ответа от базы данных
        const mockLastAppliedResponse = [[{ lastApplied0: new Date().getTime() }]];
        const mockStaticsResponse = [[{ q01: "0;0" }]];
        
        // Подменяем методы базы данных
        const connections = require('../server');
        connections.users.execute = jest.fn().mockResolvedValue(mockLastAppliedResponse);
        connections.calculator.execute = jest.fn()
            .mockResolvedValueOnce(mockStaticsResponse) // для получения столбцов
            .mockResolvedValueOnce([{ data: "" }]); // для обновления данных

        const response = await request(app)
            .post('/setEcoData')
            .query({ cuid: mockUserId, oauth: 'valid' })
            .send(mockData);

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('message', expect.any(String)); // проверяем наличие сообщения
    });
    
    // Добавьте другие тесты по мере необходимости
});
