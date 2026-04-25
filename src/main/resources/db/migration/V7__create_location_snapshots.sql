-- V7__create_location_snapshots.sql
CREATE TABLE location_snapshots (
    id              VARCHAR(36)   PRIMARY KEY,
    sos_session_id  VARCHAR(36)   NOT NULL REFERENCES active_sos(session_id),
    user_id         VARCHAR(36)   NOT NULL REFERENCES users(id),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    accuracy        REAL,
    captured_at     BIGINT        NOT NULL,
    sha256_hash     VARCHAR(64)   NOT NULL,
    previous_hash   VARCHAR(64)   NOT NULL,
    chain_hash      VARCHAR(64)   NOT NULL
);

CREATE INDEX idx_snapshots_session_id
    ON location_snapshots(sos_session_id);

CREATE INDEX idx_snapshots_captured_at
    ON location_snapshots(captured_at);