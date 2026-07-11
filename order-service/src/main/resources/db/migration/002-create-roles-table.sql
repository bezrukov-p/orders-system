-- liquibase formatted sql

-- changeset bezrukov-p:create_roles_table
-- comment: Создание таблицы roles
CREATE TABLE IF NOT EXISTS roles
(
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE INDEX idx_roles_name ON roles (name);

INSERT INTO roles (id, name)
VALUES (gen_random_uuid(), 'ROLE_USER'),
       (gen_random_uuid(), 'ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

-- rollback DELETE FROM roles WHERE name IN ('ROLE_USER', 'ROLE_ADMIN');
-- rollback DROP INDEX IF EXISTS idx_roles_name;
-- rollback DROP TABLE IF EXISTS roles;