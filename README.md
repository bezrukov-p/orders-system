# Orders System

Микросервисный pet-project: заказы, склад, уведомления.  
Event-driven архитектура на Kafka + outbox pattern.  
Полный observability: логи, метрики, трейсы.

## Стек

- Java 25, Spring Boot 4
- PostgreSQL, Kafka
- OpenTelemetry (javaagent + collector) → Prometheus / Loki / Jaeger → Grafana
- Docker Compose, Liquibase, JUnit 5 + Testcontainers

## Запуск

```bash
cd infra
cp .env.example .env
```
заполнить секреты в .env
```bash
docker compose -f docker-compose.infra.yml -f docker-compose.services.yml up -d --build
```

## Полезные ссылки

| Сервис | URL |
|---|---|
| Grafana | http://localhost:3000 (admin/admin) |
| Prometheus | http://localhost:9091 |
| Jaeger UI | http://localhost:16686 |
| Kafka UI | http://localhost:8085 |
| order-service (Swagger) | http://localhost:8080/swagger-ui.html |
| inventory-service (Swagger) | http://localhost:8081/swagger-ui.html |
| notification-service (Swagger) | http://localhost:8082/swagger-ui.html |

## Как работает система

Три сервиса общаются через Kafka. Заказы создаются по паттерну outbox — без потери событий при падении сервиса.

### Сценарий создания заказа

1. **order-service** получает `POST /api/order`:
    - создаёт заказ в статусе `PENDING`.
    - кладёт `ReserveStockCommand` в таблицу `outbox_messages`.
    - возвращает заказ клиенту, не дожидаясь резервирования.

2. **OutboxScheduler** асинхронно читает outbox и публикует команду в топик `order-commands`.

3. **inventory-service** читает `order-commands` и резервирует товары:
    - защита от race condition: атомарный `UPDATE ... WHERE quantity >= ?`.
    - защита от deadlock: списание в порядке возрастания `productId`.
    - идемпотентность по `idempotencyKey`.
    - публикует `StockReservedEvent` в топик `inventory-events`.

4. **order-service** читает `inventory-events`:
    - `success = true` → заказ `CONFIRMED`, сохраняет позиции и `totalPrice`, публикует `OrderConfirmedEvent` в `order-events`.
    - `success = false` → заказ `REJECTED`.

5. **notification-service** читает `order-events` и сохраняет проекцию заказа в свою БД.
