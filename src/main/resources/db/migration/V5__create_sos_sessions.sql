-- V5__create_sos_sessions.sql
CREATE TABLE active_sos (
    session_id      VARCHAR(36)   PRIMARY KEY,
    user_id         VARCHAR(36)   NOT NULL REFERENCES users(id),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    h3_index        VARCHAR(20)   NOT NULL,
    battery_level   INTEGER,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    triggered_at    BIGINT        NOT NULL,
    resolved_at     BIGINT
);

CREATE INDEX idx_sos_user_id      ON active_sos(user_id);
CREATE INDEX idx_sos_status       ON active_sos(status);
CREATE INDEX idx_sos_triggered_at ON active_sos(triggered_at);

-- Add to V5__create_sos_sessions.sql
ALTER TABLE active_sos ADD COLUMN geom GEOMETRY(Point, 4326);
CREATE INDEX idx_sos_geom ON active_sos USING GIST(geom);