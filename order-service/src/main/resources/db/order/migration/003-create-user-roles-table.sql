-- liquibase formatted sql

-- changeset bezrukov-p:create_user_roles_table
-- comment: Создание связующей таблицы user_roles
CREATE TABLE IF NOT EXISTS user_roles
(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

-- rollback DROP INDEX IF EXISTS idx_user_roles_role_id;
-- rollback DROP INDEX IF EXISTS idx_user_roles_user_id;
-- rollback DROP TABLE IF EXISTS user_roles;