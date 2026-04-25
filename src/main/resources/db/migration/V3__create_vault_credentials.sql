-- V3__create_vault_credentials.sql
CREATE TABLE vault_credentials (
    id                  VARCHAR(36)  PRIMARY KEY,
    user_id             VARCHAR(36)  NOT NULL UNIQUE REFERENCES users(id),
    hashed_pin          VARCHAR(255) NOT NULL,
    biometric_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          BIGINT       NOT NULL,
    updated_at          BIGINT       NOT NULL
);