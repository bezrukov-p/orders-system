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


-- rollback DROP TABLE IF EXISTS orders;