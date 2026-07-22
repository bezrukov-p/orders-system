-- liquibase formatted SQL

-- changeset bezrukov-p:create-order-items-table
-- comment: создание таблицы order_items

CREATE TABLE IF NOT EXISTS order_items
(
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT           NOT NULL,
    product_id   BIGINT           NOT NULL,
    quantity     BIGINT           NOT NULL,
    price        DOUBLE PRECISION NOT NULL,
    sale_percent INTEGER   DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE
);