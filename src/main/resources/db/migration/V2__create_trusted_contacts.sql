-- V2__create_trusted_contacts.sql
CREATE TABLE trusted_contacts (
    id              VARCHAR(36)  PRIMARY KEY,
    user_id         VARCHAR(36)  NOT NULL REFERENCES users(id),
    name            VARCHAR(100) NOT NULL,
    phone_number    VARCHAR(15)  NOT NULL,
    fcm_token       VARCHAR(255),
    created_at      BIGINT       NOT NULL
);

CREATE INDEX idx_trusted_contacts_user_id
    ON trusted_contacts(user_id);