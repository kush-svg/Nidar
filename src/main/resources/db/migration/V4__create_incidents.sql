-- V4__create_incidents.sql
CREATE TABLE incidents (
    id              VARCHAR(36)   PRIMARY KEY,
    user_id         VARCHAR(36)   NOT NULL REFERENCES users(id),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    h3_index        VARCHAR(20)   NOT NULL,
    incident_type   VARCHAR(30)   NOT NULL,
    base_weight     INTEGER       NOT NULL,
    trust_score     DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    timestamp       BIGINT        NOT NULL
);

-- Composite index for bounding box queries
CREATE INDEX idx_incidents_lat_lng
    ON incidents(latitude, longitude);

-- Index for H3 cell queries
CREATE INDEX idx_incidents_h3
    ON incidents(h3_index);

-- Index for time filtering
CREATE INDEX idx_incidents_timestamp
    ON incidents(timestamp);

-- PostGIS geometry column for advanced spatial queries
SELECT AddGeometryColumn('incidents', 'geom', 4326, 'POINT', 2);

CREATE INDEX idx_incidents_geom
    ON incidents USING GIST(geom);