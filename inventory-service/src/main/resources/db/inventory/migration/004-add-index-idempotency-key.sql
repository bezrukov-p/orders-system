-- liquibase formatted sql

-- changeset bezrukov-p:add_index_idempotency_key
-- comment: Добавление индекса на поле key
CREATE UNIQUE INDEX idx_idempotency_keys_key ON idempotency_keys (key);