# ADR-0032: Multi-Agent Collaboration Framework

## Status

Accepted

## Context

Nova Platform orchestrates individual agents (Planner, Coding, Review, Testing, etc.) through the existing orchestration engine. Operators need multiple agents to collaborate on the same run with shared context, immutable message history, parallel work groups, conflict detection, and human intervention — without replacing orchestration or introducing distributed messaging.

Requirements:

- Coordinate existing agents within collaboration sessions
- Immutable messages and append-only timeline
- Shared read-only context (project, branch, release, environment, execution, audit refs)
- Parallel participant groups with coordinator gate
- Artifact conflict detection
- Audit integration via existing Audit Center
- HTTP polling only (no WebSockets/Kafka/Redis)
- Collaboration-specific tables only; no duplication of orchestration tables

## Decision

Implement `ai.nova.platform.collaboration` on Platform API with:

1. **CollaborationService** — session CRUD, messages, decisions, pause/resume/cancel facade
2. **CollaborationCoordinator** — task assignment lifecycle, parallel groups, conflict detection on `artifactRef`
3. **CollaborationSessionService** — load and map session aggregates
4. **CollaborationTimelineService** — append-only timeline events
5. **CollaborationAuthorizationService** — `COLLABORATION_READ`, `COLLABORATION_WRITE`, `COLLABORATION_ADMIN`
6. **CollaborationController** — REST API under `/api/collaboration`
7. **Portal** — `/collaboration` with Material tabs and 10s polling
8. **Flyway V55** — collaboration tables + permissions + `AuditSource.COLLABORATION`

Sessions optionally link to `orchestration_run_id` for correlation with existing runs.

## Consequences

### Positive

- Multiple agents can collaborate in one session with full audit trail
- Conflict detection prevents silent artifact collisions
- Human reviewers can pause/resume/cancel via admin permissions
- No new infrastructure beyond Spring Boot and PostgreSQL

### Negative

- Coordinator logic is in-process; no cross-instance collaboration coordination
- Agent execution still flows through orchestration; collaboration does not dispatch agents directly
- Message volume capped by `nova.collaboration.max-messages`

### Alternatives considered

- **Replace orchestration engine** — rejected (explicit constraint)
- **Kafka/RabbitMQ event bus** — rejected (explicit constraint)
- **WebSocket live updates** — rejected (HTTP polling only)

## References

- [Multi-Agent Collaboration](../043_MULTI_AGENT_COLLABORATION.md)
- [Multi-Agent Orchestration Foundation](../022_MULTI_AGENT_ORCHESTRATION_FOUNDATION.md)
- [Enterprise Dashboard](../042_ENTERPRISE_DASHBOARD.md)
