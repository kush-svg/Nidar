-- V6__create_evidence_items.sql
CREATE TABLE evidence_items (
    id                  VARCHAR(36)   PRIMARY KEY,
    user_id             VARCHAR(36)   NOT NULL REFERENCES users(id),
    sos_session_id      VARCHAR(36)   REFERENCES active_sos(session_id),
    type                VARCHAR(20)   NOT NULL,
    capture_mode        VARCHAR(10)   NOT NULL,
    minio_object_key    VARCHAR(512)  NOT NULL,
    file_size_bytes     BIGINT,
    sha256_hash         VARCHAR(64)   NOT NULL,
    previous_hash       VARCHAR(64)   NOT NULL,
    chain_hash          VARCHAR(64)   NOT NULL,
    captured_at         BIGINT        NOT NULL,
    uploaded_at         BIGINT        NOT NULL,
    device_id           VARCHAR(100),
    battery_level       INTEGER,
    network_type        VARCHAR(10),
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING_REVIEW'
);

CREATE INDEX idx_evidence_user_id
    ON evidence_items(user_id);

CREATE INDEX idx_evidence_sos_session
    ON evidence_items(sos_session_id);

CREATE INDEX idx_evidence_status
    ON evidence_items(status);

CREATE INDEX idx_evidence_uploaded_at
    ON evidence_items(uploaded_at);