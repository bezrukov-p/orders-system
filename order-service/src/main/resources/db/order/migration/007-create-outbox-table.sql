-- liquibase formatted sql

-- changeset bezrukov-p:create_outbox_messages_table

CREATE TABLE IF NOT EXISTS outbox_messages
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB        NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    processed       BOOLEAN          DEFAULT FALSE,
    processed_at    TIMESTAMP,
    retry_count     INT              DEFAULT 0
);

CREATE INDEX idx_outbox_messages_unprocessed
    ON outbox_messages (created_at)
    WHERE processed = false;