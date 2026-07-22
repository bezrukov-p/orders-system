-- liquibase formatted sql

-- changeset bezrukov-p:create_products_table
-- comment: Создание таблицы products
CREATE TABLE IF NOT EXISTS products
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255)   NOT NULL,
    quantity     BIGINT         NOT NULL,
    price        DOUBLE PRECISION,
    sale_percent INTEGER   DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);
CREATE INDEX IF NOT EXISTS idx_products_price ON products (price);
CREATE INDEX IF NOT EXISTS idx_products_sale_percent ON products (sale_percent);

-- rollback DROP INDEX IF EXISTS idx_products_name;
-- rollback DROP INDEX IF EXISTS idx_products_price;
-- rollback DROP INDEX IF EXISTS idx_products_sale_percent;
-- rollback DROP TABLE IF EXISTS products;