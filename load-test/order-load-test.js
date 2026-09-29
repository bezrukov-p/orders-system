import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// =============================================
// Кастомные метрики
// =============================================
const orderCreateErrors = new Rate('order_create_errors');
const orderCreateTime   = new Trend('order_create_time');
const ordersCreated     = new Counter('orders_created');

// =============================================
// Конфигурация
// =============================================
const BASE_URL = 'http://localhost:8080';

// ⚠️ ВСТАВЬ СВОЙ JWT ТОКЕН (получи один раз через Postman/curl)
const TOKEN = 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ4eHgiLCJpYXQiOjE3OTA2OTM2MjMsInVzZXJJZCI6IjZiNDA0Y2E3LWY5NWUtNGUxOS04NDVhLWU3NDg4YzI1MTE0ZCIsInJvbGVzIjpbIlVTRVIiXSwiZXhwIjoxNzk5MzMzNjIzfQ.7JNGthejg4ryTPNauVOHoI_enE570shRWUs0aY-IlEQCY15ytlFkXYr87kMQWrKd8UZJIs4MKgvMj86g78PR0w'
// ⚠️ ЗАМЕНИ на реальные id товаров из твоей БД
const PRODUCTS = [
    { id: 1 },
    { id: 2 },
    { id: 3 },
    { id: 4 },
    { id: 5 },
];

// =============================================
// Опции — НАГРУЗКА
// =============================================
export const options = {
    scenarios: {
        order_load: {
            executor: 'ramping-arrival-rate',  // ← фиксированный RPS!
            startRate: 10,                      // 10 RPS в начале
            timeUnit: '1s',
            preAllocatedVUs: 50,                // VU для разгона
            maxVUs: 500,                        // максимум VU
            stages: [
                { duration: '1m', target: 200 },
                { duration: '1m', target: 220 },
                { duration: '1m', target: 240 },
                { duration: '1m', target: 260 },
                { duration: '1m', target: 0 },    // спад
            ],
            exec: 'createOrder',
        },
    },
    thresholds: {
        'order_create_errors': ['rate<0.05'],   // < 5% ошибок
        'http_req_duration':   ['p(95)<3000'],  // p95 < 3 сек
        'http_req_failed':     ['rate<0.05'],
    },
    discardResponseBodies: false,
};

// =============================================
// Хелперы
// =============================================
function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pickUniqueProducts(n) {
    const shuffled = PRODUCTS.slice().sort(() => Math.random() - 0.5);
    return shuffled.slice(0, Math.min(n, PRODUCTS.length));
}

// =============================================
// Создание заказа
// =============================================
export function createOrder() {
    // 1–5 уникальных товаров
    const itemCount = randomInt(1, Math.min(5, PRODUCTS.length));
    const selectedProducts = pickUniqueProducts(itemCount);

    const items = selectedProducts.map((p) => ({
        productId: p.id,
        quantity: randomInt(1, 5),
    }));

    // Уникальный ключ идемпотентности
    const idempotencyKey = `load-${__VU}-${__ITER}-${Date.now()}-${Math.random()}`;

    const payload = JSON.stringify({ idempotencyKey, items });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${TOKEN}`,
        },
        tags: { name: 'CreateOrder' },
    };

    const res = http.post(`${BASE_URL}/api/order`, payload, params);

    orderCreateTime.add(res.timings.duration);

    const ok = check(res, {
        'create order: 201': (r) => r.status === 201,
        'create order: has id': (r) => {
            try { return !!JSON.parse(r.body).id; } catch { return false; }
        },
    });

    orderCreateErrors.add(!ok);

    if (!ok) {
        console.error(`❌ Create order failed: status=${res.status}, body=${res.body}`);
    } else {
        ordersCreated.add(1);
    }
}

// =============================================
// Итоговый отчёт
// =============================================
export function handleSummary(data) {
    return {
        'order-load-results.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}