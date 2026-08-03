-- liquibase formatted sql

-- changeset bezrukov-p:create_idempotency_keys_table
-- comment: Создание таблицы idempotency_keys
CREATE TABLE IF NOT EXISTS idempotency_keys
(
    id           BIGSERIAL PRIMARY KEY,
    key          VARCHAR(255)   NOT NULL
);


-- rollback DROP TABLE IF EXISTS idempotency_keys;