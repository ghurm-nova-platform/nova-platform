# ADR-0012: Durable Agent Orchestration Foundation

## Status

Accepted (Sprint 2 Phase 1)

## Context

Nova needs multi-agent workflows with durable state, dependency graphs, retries, and cancellation that compose with the existing Agent Runtime and Model Gateway. Introducing Temporal, Camunda, or a message bus in Sprint 2 Phase 1 would add operational complexity before domain agents exist.

## Decision

1. Persist orchestration runs, tasks, dependencies, attempts, and events in PostgreSQL (Flyway V29–V32) as the durable source of truth.
2. Use short TX1 / external AI work / short TX2 boundaries with optimistic locking and stale-result rejection (aligned with connection-test and catalog sync).
3. Claim tasks with atomic compare-and-set updates and time-bounded leases so multiple application nodes are safe without JVM-only locks.
4. Keep scheduler and executor as replaceable Spring components (`nova.orchestration.*`) so an external queue or workflow engine can be introduced later without rewriting the domain model.
5. Do **not** adopt Kafka, RabbitMQ, Redis, Temporal, or Camunda in this phase.

## Alternatives considered

- **External workflow engine now** — deferred; high ops cost before specialized agents exist.
- **In-memory orchestration only** — rejected; loses durability and multi-node safety.
- **Long DB transactions around provider calls** — rejected; blocks pools and breaks cancel/stale semantics.

## Consequences

### Positive

- Auditable, tenant-isolated runs with event timelines.
- Compatible with existing gateway cancellation, timeout, vault, and SSRF guarantees.
- Clear upgrade path to an external durable worker.

### Negative and risks

- In-process polling requires careful lease tuning and horizontal-scale claim contention.
- Graph features are foundational; recursive agent spawning remains future work.

## Security and privacy impact

RBAC `ORCHESTRATION_*`; org/project scoped queries; sanitized events/errors; no credential persistence.

## Follow-up actions

- [ ] Optional external queue adapter behind claim/execute interfaces
- [ ] Human approval UI for `HUMAN_APPROVAL` tasks
- [ ] Specialized Planner / Coding / Review agents in later Sprint 2 phases
