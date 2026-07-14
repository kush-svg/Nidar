-- V9__add_geom_to_users.sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS geom GEOMETRY(Point, 4326);
