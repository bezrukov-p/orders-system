-- liquibase formatted sql

-- changeset bezrukov-p:create_users_table
-- comment: Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       username VARCHAR(50) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL
);

-- rollback DROP TABLE users;