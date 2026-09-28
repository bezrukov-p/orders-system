-- liquibase formatted sql

-- changeset bezrukov-p:add_column_product_name

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS name VARCHAR(100);