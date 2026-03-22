-- V1: Initial schema — mirrors JPA entity definitions

CREATE TABLE users (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(72),
    role     VARCHAR(255) NOT NULL DEFAULT 'USER',
    enabled  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by  BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE project_members (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects(id),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    role        VARCHAR(50) NOT NULL DEFAULT 'EDITOR',
    created_at  TIMESTAMP,
    UNIQUE (project_id, user_id)
);

CREATE TABLE tasks (
    id           BIGSERIAL PRIMARY KEY,
    version      BIGINT,
    title        VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    status       VARCHAR(255) NOT NULL DEFAULT 'OPEN',
    priority     VARCHAR(255) NOT NULL DEFAULT 'MEDIUM',
    start_date   DATE,
    due_date     DATE,
    completed_at TIMESTAMP,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    project_id   BIGINT NOT NULL REFERENCES projects(id),
    user_id      BIGINT REFERENCES users(id)
);

CREATE TABLE checklist_items (
    id         BIGSERIAL PRIMARY KEY,
    text       VARCHAR(200) NOT NULL,
    checked    BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    task_id    BIGINT NOT NULL REFERENCES tasks(id)
);

CREATE TABLE tags (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE task_tags (
    task_id BIGINT NOT NULL REFERENCES tasks(id),
    tag_id  BIGINT NOT NULL REFERENCES tags(id),
    PRIMARY KEY (task_id, tag_id)
);

CREATE TABLE comments (
    id         BIGSERIAL PRIMARY KEY,
    text       VARCHAR(500) NOT NULL,
    created_at TIMESTAMP,
    task_id    BIGINT NOT NULL REFERENCES tasks(id),
    user_id    BIGINT NOT NULL REFERENCES users(id)
);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    action      VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255),
    entity_id   BIGINT,
    principal   VARCHAR(255) NOT NULL,
    details     TEXT,
    timestamp   TIMESTAMP NOT NULL
);

CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    actor_id   BIGINT REFERENCES users(id),
    type       VARCHAR(255) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    link       VARCHAR(500),
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE settings (
    id            BIGSERIAL PRIMARY KEY,
    setting_key   VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(500)
);

CREATE TABLE user_preferences (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    pref_key   VARCHAR(100) NOT NULL,
    pref_value VARCHAR(500),
    UNIQUE (user_id, pref_key)
);

-- Seed a default admin user for first login
-- Password: 'password' (BCrypt encoded)
-- Change this password after first login via the profile page
INSERT INTO users (name, email, password, role, enabled)
VALUES ('Admin', 'admin@example.com', '$2b$10$eKasj4ieP4KxeTTOYcMBh.9MHNSWIKjRc5lcTWr7Um8K5m/CJIux2', 'ADMIN', true);
