-- liquibase formatted sql

-- changeset bezrukov-p:create_users_table
-- comment: Создание таблицы users
CREATE TABLE IF NOT EXISTS users
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- rollback DROP INDEX IF EXISTS idx_users_email;
-- rollback DROP INDEX IF EXISTS idx_users_username;
-- rollback DROP TABLE IF EXISTS users;