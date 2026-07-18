-- Concurrency-safe event sequence counters (isolated from run.version)
-- and a singleton row used as a short claim-capacity lock.

CREATE TABLE agent_orchestration_event_counters (
    run_id UUID PRIMARY KEY,
    next_sequence BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aoec_run FOREIGN KEY (run_id) REFERENCES agent_orchestration_runs (id),
    CONSTRAINT chk_aoec_next_sequence CHECK (next_sequence >= 0)
);

INSERT INTO agent_orchestration_event_counters (run_id, next_sequence)
SELECT id, event_sequence
FROM agent_orchestration_runs;

CREATE TABLE agent_orchestration_claim_lock (
    id SMALLINT PRIMARY KEY,
    CONSTRAINT chk_aocl_id CHECK (id = 1)
);

INSERT INTO agent_orchestration_claim_lock (id) VALUES (1);
