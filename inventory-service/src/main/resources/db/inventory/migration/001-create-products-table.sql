-- liquibase formatted sql

-- changeset bezrukov-p:create_products_table
-- comment: Создание таблицы products
CREATE TABLE IF NOT EXISTS products
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255)   NOT NULL,
    quantity     BIGINT         NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    price        DOUBLE PRECISION NOT NULL CHECK (price >= 0),
    sale_percent INTEGER   DEFAULT 0 CHECK (sale_percent BETWEEN 0 AND 100),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);

-- rollback DROP INDEX IF EXISTS idx_products_name;
-- rollback DROP TABLE IF EXISTS products;