-- V8__create_audit_log.sql
CREATE TABLE evidence_audit_log (
    id                  VARCHAR(36)   PRIMARY KEY,
    evidence_item_id    VARCHAR(36)   REFERENCES evidence_items(id),
    user_id             VARCHAR(36)   NOT NULL REFERENCES users(id),
    action              VARCHAR(30)   NOT NULL,
    performed_at        BIGINT        NOT NULL,
    file_hash           VARCHAR(64),
    chain_hash          VARCHAR(64)
);

-- Append-only enforcement at DB level
-- Prevents DELETE and UPDATE on this table
CREATE RULE no_delete_audit AS ON DELETE TO evidence_audit_log
    DO INSTEAD NOTHING;

CREATE RULE no_update_audit AS ON UPDATE TO evidence_audit_log
    DO INSTEAD NOTHING;

CREATE INDEX idx_audit_evidence_item_id
    ON evidence_audit_log(evidence_item_id);

CREATE INDEX idx_audit_user_id
    ON evidence_audit_log(user_id);

CREATE INDEX idx_audit_performed_at
    ON evidence_audit_log(performed_at);
