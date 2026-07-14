const CALCULATORS = ['Пища', 'Вода', 'Отходы', 'Энергия', 'Транспорт'];

const KS = [2.5 / 1000, 0.15 / 1000, 1 / 1000, 0.5, 0.25];

// Итог каждого калькулятора после умножения на KS выражен в кг CO2 (не в тоннах).
const CARBON_UNIT = 'кг CO2';

const CALC_COLORS = ['85,255,85', '85,85,255', '0,0,0', '154,205,50', '188,143,143'];

const SPECIFY_AVAILABLE = ['0_0', '0_1', '0_2'];

const SPECIFY = {
    '00': {
        0: { name: 'Говядина', color: '255, 87, 51' }, 1: { name: 'Свинина', color: '51, 255, 87' },
        2: { name: 'Курица', color: '51, 87, 255' }, 3: { name: 'Баранина', color: '255, 51, 153' },
        4: { name: 'Индейка', color: '255, 204, 0' }, 5: { name: 'Утка', color: '210, 247, 166' },
        6: { name: 'Кролик', color: '199, 0, 57' }, 7: { name: 'Рыба', color: '144, 12, 63' },
        8: { name: 'Морепродукты', color: '88, 24, 69' },
    },
    '01': { 0: { name: 'Овощи', color: '88, 24, 69' }, 1: { name: 'Фрукты', color: '199, 0, 57' } },
    '02': {
        0: { name: 'Готовые продукты', color: '255, 51, 153' },
        1: { name: 'Закуски', color: '144, 12, 63' },
        2: { name: 'С добавками', color: '51, 87, 255' },
    },
};

const MSK_TODAY = `(now() AT TIME ZONE 'Europe/Moscow')::date`;

function mskTodayString() {
    return new Date().toLocaleDateString('en-CA', { timeZone: 'Europe/Moscow' });
}

function lastDays(n) {
    const base = new Date(mskTodayString() + 'T00:00:00Z');
    const keys = [];
    const labels = [];
    for (let d = n - 1; d >= 0; --d) {
        const day = new Date(base);
        day.setUTCDate(base.getUTCDate() - d);
        keys.push(day.toISOString().slice(0, 10));
        labels.push(`${day.getUTCDate()}.${day.getUTCMonth() + 1}`);
    }
    return { keys, labels };
}

module.exports = {
    CALCULATORS, KS, CARBON_UNIT, CALC_COLORS, SPECIFY, SPECIFY_AVAILABLE,
    MSK_TODAY, mskTodayString, lastDays,
};
