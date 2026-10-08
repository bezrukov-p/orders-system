-- liquibase formatted sql

-- changeset bezrukov-p:add_column_failed

ALTER TABLE outbox_messages ADD COLUMN IF NOT EXISTS failed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_outbox_pending
    ON outbox_messages (created_at)
    WHERE processed = false AND failed = false;