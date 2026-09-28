-- liquibase formatted sql

-- changeset bezrukov-p:create_refreshtokens_table
-- comment: Создание таблицы refresh_tokens
CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE
    );

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens (expiry_date) WHERE revoked = false;

-- rollback DROP INDEX IF EXISTS idx_refresh_tokens_expiry_date;
-- rollback DROP INDEX IF EXISTS idx_refresh_tokens_user_id;