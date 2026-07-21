-- Enterprise Audit Center — database-level immutability (Sprint 4 Phase 6 review).
-- Applied by Flyway Java migration V51__audit_database_immutability on PostgreSQL.
-- H2 test runs install equivalent Java triggers via the same migration.
--
-- Protects append-only tables from UPDATE and DELETE.
-- Does not block INSERT.
-- Does not block audit_sessions.ended_at updates (session closure).
-- Does not block audit_entities.display_label / updated_at (mutable reference registry).

ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_action;
ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_source;

ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_action CHECK (action IN (
    'CREATE', 'UPDATE', 'DELETE', 'ENABLE', 'DISABLE', 'ARCHIVE', 'APPROVE', 'REJECT',
    'MERGE', 'VALIDATE', 'OBSERVE', 'START', 'COMPLETE', 'FAIL', 'PREPARE', 'READY', 'PUBLISH',
    'LOGIN', 'LOGOUT', 'ACCESS'
));

ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_source CHECK (source IN (
    'PORTAL', 'REST_API', 'SYSTEM', 'SCHEDULER', 'MERGE_AGENT', 'RELEASE_MANAGER',
    'DEPLOYMENT_OBSERVATION', 'ROLLBACK_MANAGER', 'RELEASE_POLICIES', 'ENVIRONMENT_MANAGEMENT',
    'ORCHESTRATION', 'PLANNER', 'CODING', 'REVIEW', 'TESTING', 'PATCH', 'GIT_INTEGRATION',
    'PULL_REQUEST', 'CI_OBSERVATION', 'REPAIR', 'APPROVAL_GATE'
));

CREATE OR REPLACE FUNCTION reject_audit_mutation() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'AUDIT_IMMUTABLE';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_events_immutable ON audit_events;
CREATE TRIGGER trg_audit_events_immutable
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE PROCEDURE reject_audit_mutation();

DROP TRIGGER IF EXISTS trg_audit_correlation_immutable ON audit_correlation;
CREATE TRIGGER trg_audit_correlation_immutable
    BEFORE UPDATE OR DELETE ON audit_correlation
    FOR EACH ROW EXECUTE PROCEDURE reject_audit_mutation();

DROP TRIGGER IF EXISTS trg_audit_indexes_immutable ON audit_indexes;
CREATE TRIGGER trg_audit_indexes_immutable
    BEFORE UPDATE OR DELETE ON audit_indexes
    FOR EACH ROW EXECUTE PROCEDURE reject_audit_mutation();
