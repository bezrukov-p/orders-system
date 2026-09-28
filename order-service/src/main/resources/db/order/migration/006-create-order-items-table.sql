-- liquibase formatted sql

-- changeset bezrukov-p:create_order_items_table
-- comment: Создание таблицы order_items
CREATE TABLE IF NOT EXISTS order_items
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID             NOT NULL,
    product_id   BIGINT           NOT NULL,
    quantity     BIGINT           NOT NULL,
    price        DOUBLE PRECISION NOT NULL,
    sale_percent INTEGER          DEFAULT 0,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

-- rollback DROP TABLE IF EXISTS order_items;