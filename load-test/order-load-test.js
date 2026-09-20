import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// =============================================
// Метрики
// =============================================
const errorRate = new Rate('errors');
const orderCreationTime = new Trend('order_creation_time');
const orderCreated = new Counter('orders_created');

// =============================================
// Конфигурация нагрузки
// =============================================
export const options = {
    stages: [
        { duration: '30s', target: 10 },    // Разогрев: 10 пользователей
        { duration: '1m', target: 50 },     // Нагрузка: 50 пользователей
        { duration: '1m', target: 100 },    // Пик: 100 пользователей
        { duration: '30s', target: 0 },     // Спад: 0
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],   // 95% запросов < 2 сек
        http_req_failed: ['rate<0.05'],       // < 5% ошибок
        errors: ['rate<0.05'],
    },
};

// =============================================
// JWT токен (получи один раз и вставь)
// =============================================
const TOKEN = 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMTEiLCJpYXQiOjE3ODk3OTU2OTQsInVzZXJJZCI6IjgzN2M4MGQ2LTY2Y2ItNGRjZC1iNDZkLTFhMTg0ZTYwYjFjNCIsInJvbGVzIjpbIlVTRVIiXSwiZXhwIjoxNzk4NDM1Njk0fQ.QAKr8onWItp5XKg9BA4Hx5_6BFSk_hASRVu6Fqg0d9_3nkK0J7Pqa8usCGgcsGaJUdkseZ56LU073ShM3voaow';  // ← твой реальный JWT

const BASE_URL = 'http://localhost:8080';

// =============================================
// Основной сценарий
// =============================================
export default function () {
    // 1. Генерируем уникальный ключ идемпотентности
    const idempotencyKey = `load-test-${Date.now()}-${Math.random()}`;

    // 2. Случайный товар
    const productId = Math.floor(Math.random() * 50) + 1;
    const quantity = Math.floor(Math.random() * 3) + 1;

    // 3. Формируем запрос
    const payload = JSON.stringify({
        idempotencyKey: idempotencyKey,
        items: [
            {
                productId: productId,
                quantity: quantity,
            },
        ],
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${TOKEN}`,
        },
        tags: {
            name: 'CreateOrder',
        },
    };

    // 4. Отправляем запрос
    const startTime = Date.now();
    const response = http.post(`${BASE_URL}/api/order`, payload, params);
    const duration = Date.now() - startTime;

    // 5. Записываем метрики
    orderCreationTime.add(duration);

    // 6. Проверяем ответ
    const success = check(response, {
        'status is 201': (r) => r.status === 201,
        'has order id': (r) => {
            try {
                return JSON.parse(r.body).id !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    if (success) {
        orderCreated.add(1);
    } else {
        errorRate.add(1);
        console.error(`❌ Failed: status=${response.status}, body=${response.body}`);
    }

    // 7. Пауза между запросами
    sleep(0.5);
}

// =============================================
// Итоговый отчёт
// =============================================
export function handleSummary(data) {
    return {
        'load-test-results.json': JSON.stringify(data),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}

import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';