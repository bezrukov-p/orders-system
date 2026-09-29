-- liquibase formatted SQL

-- changeset bezrukov-p:create_orders_table
-- comment: Создание таблицы orders
CREATE TABLE IF NOT EXISTS orders
(
    id          BIGSERIAL PRIMARY KEY,
    order_id    UUID NOT NULL UNIQUE,
    user_id     UUID NOT NULL,
    user_email  VARCHAR(255),
    description TEXT,
    status      VARCHAR(50),
    total_price DOUBLE PRECISION,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для типичных запросов
CREATE INDEX idx_notif_orders_user_id ON orders (user_id);
CREATE INDEX idx_notif_orders_created_at ON orders (created_at DESC);

-- rollback DROP INDEX IF EXISTS idx_notif_orders_created_at;
-- rollback DROP INDEX IF EXISTS idx_notif_orders_user_id;
-- rollback DROP TABLE IF EXISTS orders;