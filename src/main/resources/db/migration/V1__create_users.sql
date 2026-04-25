-- V1__create_users.sql
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE users (
    id              VARCHAR(36)  PRIMARY KEY,
    phone_number    VARCHAR(15)  NOT NULL UNIQUE,
    name            VARCHAR(100),
    fcm_token       VARCHAR(255),
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    h3_index        VARCHAR(20),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_seen_at    BIGINT,
    created_at      BIGINT       NOT NULL
);

CREATE INDEX idx_users_phone      ON users(phone_number);
CREATE INDEX idx_users_h3_index   ON users(h3_index);
CREATE INDEX idx_users_role       ON users(role);