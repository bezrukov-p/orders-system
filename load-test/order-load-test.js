import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// =============================================
// Кастомные метрики
// =============================================
const registerErrors = new Rate('register_errors');
const loginErrors = new Rate('login_errors');
const orderCreateErrors = new Rate('order_create_errors');
const orderStatusErrors = new Rate('order_status_errors');
const idempotencyViolations = new Rate('idempotency_violations');

const registerTime = new Trend('register_duration');
const loginTime = new Trend('login_duration');
const orderCreateTime = new Trend('order_create_duration');
const orderFinalizeTime = new Trend('order_finalize_duration'); // pending → confirmed

const ordersCreated = new Counter('orders_created');
const ordersConfirmed = new Counter('orders_confirmed');
const usersRegistered = new Counter('users_registered');

// =============================================
// Конфигурация нагрузки
// =============================================
export const options = {
    stages: [
        { duration: '30s', target: 10 },
        //{ duration: '1m',  target: 50 },
        //{ duration: '1m',  target: 100 },
        { duration: '1m',  target: 200 },
        { duration: '1m',  target: 300 },
        //{ duration: '1m',  target: 400 },
        //{ duration: '1m',  target: 500 },
        { duration: '30s', target: 0 },     // ramp-down
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.05'],
        register_errors: ['rate<0.01'],
        login_errors: ['rate<0.01'],
        order_create_errors: ['rate<0.05'],
        order_status_errors: ['rate<0.10'],   // статус может не успеть стать confirmed
        idempotency_violations: ['rate<0.001'],
    },
    // Если бэкенд не тянет 500 VU — уменьши. Смотри корреляцию с Grafana.
    // Для локальной машины реально ~100-200 VU макс.
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = 'LoadTest123!';

// =============================================
// Хелпер: заголовки с токеном
// =============================================
function authHeaders(token) {
    return {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
    };
}

// =============================================
// Хелпер: регистрация (если 409 — считаем успехом)
// =============================================
function register(username) {
    const payload = JSON.stringify({
        username,
        password: PASSWORD,
        email: `${username}@load.test`,
    });

    const res = http.post(`${BASE_URL}/api/auth/register`, payload, {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'Register' },
    });

    registerTime.add(res.timings.duration);

    // 201 — создан, 409 — уже существует (тоже ок, значит логин сработает)
    const ok = check(res, {
        'register: status 201 or 409': (r) => r.status === 201 || r.status === 409,
    });

    registerErrors.add(!ok);
    if (ok && res.status === 201) {
        usersRegistered.add(1);
    }
    return ok;
}

// =============================================
// Хелпер: логин → JWT
// =============================================
function login(username) {
    const payload = JSON.stringify({ username, password: PASSWORD });

    const res = http.post(`${BASE_URL}/api/auth/login`, payload, {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'Login' },
    });

    loginTime.add(res.timings.duration);

    const ok = check(res, {
        'login: status 200': (r) => r.status === 200,
        'login: has token': (r) => {
            try { return !!JSON.parse(r.body).token; } catch { return false; }
        },
    });

    loginErrors.add(!ok);

    if (!ok) return null;

    try {
        return JSON.parse(res.body).token;
    } catch {
        return null;
    }
}

// =============================================
// Хелпер: создание заказа
// =============================================
function createOrder(token, idempotencyKey) {
    const payload = JSON.stringify({
        idempotencyKey,
        items: [{
            productId: Math.floor(Math.random() * 50) + 1,
            quantity: Math.floor(Math.random() * 3) + 1,
        }],
    });

    const res = http.post(`${BASE_URL}/api/order`, payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
        tags: { name: 'CreateOrder' },
    });

    orderCreateTime.add(res.timings.duration);

    const ok = check(res, {
        'create: status 201': (r) => r.status === 201,
        'create: has order id': (r) => {
            try { return !!JSON.parse(r.body).id; } catch { return false; }
        },
    });

    orderCreateErrors.add(!ok);

    if (!ok) return null;

    try {
        return JSON.parse(res.body).id;
    } catch {
        return null;
    }
}

// =============================================
// Хелпер: проверка статуса (polling до confirmed)
// =============================================
function pollOrderStatus(token, orderId, maxAttempts = 20, delaySec = 0.5) {
    for (let attempt = 0; attempt < maxAttempts; attempt++) {
        const res = http.get(`${BASE_URL}/api/order/${orderId}`, {
            headers: { 'Authorization': `Bearer ${token}` },
            tags: { name: 'GetOrderStatus' },
        });

        if (res.status !== 200) {
            orderStatusErrors.add(1);
            return null;
        }

        try {
            const body = JSON.parse(res.body);
            const status = body.status || body.state;

            if (status === 'CONFIRMED' || status === 'confirmed') {
                orderStatusErrors.add(0);
                return 'CONFIRMED';
            }
            if (status === 'CANCELLED' || status === 'cancelled') {
                orderStatusErrors.add(1);
                return 'CANCELLED';
            }
            // PENDING — ждём дальше
        } catch (e) {
            orderStatusErrors.add(1);
            return null;
        }

        sleep(delaySec);
    }
    // не дождались — тоже плохо, но не критично
    orderStatusErrors.add(1);
    return 'TIMEOUT';
}

// =============================================
// Хелпер: повторный запрос с тем же idempotencyKey
// =============================================
function checkIdempotency(token, idempotencyKey, originalOrderId) {
    const payload = JSON.stringify({
        idempotencyKey,
        items: [{ productId: 1, quantity: 1 }],
    });

    const res = http.post(`${BASE_URL}/api/order`, payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
        tags: { name: 'CreateOrderIdempotent' },
    });

    // Ожидаем: тот же order id, либо 409 Conflict
    let ok = false;
    if (res.status === 200 || res.status === 201) {
        try {
            const body = JSON.parse(res.body);
            ok = body.id === originalOrderId;
        } catch { ok = false; }
    } else if (res.status === 409) {
        ok = true;
    }

    idempotencyViolations.add(!ok);
}

// =============================================
// Основной сценарий VU
// =============================================
export default function () {
    const vuId = __VU;
    const iterId = __ITER;
    const username = `loadtest_${vuId}_${iterId}`;

    let token = null;
    let orderId = null;
    const idempotencyKey = `idem-${vuId}-${iterId}-${Date.now()}`;

    // ---- 1. Регистрация ----
    group('1. Register', () => {
        if (!register(username)) return;
    });

    // ---- 2. Логин ----
    group('2. Login', () => {
        token = login(username);
    });

    if (!token) {
        sleep(1);
        return;   // без токена дальше смысла нет
    }

    // ---- 3. Создание заказа ----
    group('3. Create order', () => {
        orderId = createOrder(token, idempotencyKey);
    });

    if (!orderId) {
        sleep(1);
        return;
    }

    ordersCreated.add(1);

    // ---- 4. Проверка идемпотентности (повторный POST с тем же ключом) ----
    group('4. Idempotency check', () => {
        checkIdempotency(token, idempotencyKey, orderId);
    });

    // ---- 5. Polling статуса до CONFIRMED ----
    group('5. Poll order status', () => {
        const finalStatus = pollOrderStatus(token, orderId);
        if (finalStatus === 'CONFIRMED') {
            ordersConfirmed.add(1);
        }
    });

    // ---- 6. Пауза ----
    sleep(0.5 + Math.random() * 1.5);   // 0.5–2 сек, чтобы VU не били синхронно
}

// =============================================
// Итоговый отчёт
// =============================================
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

export function handleSummary(data) {
    return {
        'load-test-results.json': JSON.stringify(data),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}