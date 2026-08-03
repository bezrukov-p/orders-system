-- liquibase formatted sql

-- changeset bezrukov-p:create_orders_table
-- comment: Создание таблицы orders
CREATE TABLE IF NOT EXISTS orders
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    status     VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_price DOUBLE PRECISION NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- rollback DROP TABLE IF EXISTS orders;