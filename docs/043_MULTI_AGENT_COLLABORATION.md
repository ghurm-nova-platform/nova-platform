# Multi-Agent Collaboration Framework

Sprint 6 Phase 1 — coordinates multiple AI agents and human reviewers within a single orchestration run. The collaboration layer wraps existing agents; it does not replace the orchestration engine or introduce distributed messaging.

## Capabilities

1. **Collaboration sessions** linked to organization, project, and optional orchestration run
2. **Participants** for Planner, Coding, Review, Testing, Repair, CI, Merge, Release, Deployment, Rollback, and Human Reviewer roles
3. **Immutable messages** (TASK, QUESTION, ANSWER, WARNING, ERROR, INFO, APPROVAL_REQUEST, DECISION)
4. **Shared context** (project, repository, branch, release, environment, execution, audit references) — read-only except through CollaborationService
5. **Task assignment** with assign, complete, reject, reassign, block, resume, cancel
6. **Parallel participant groups** (e.g. Coding + Review + Documentation) with coordinator gate before merge
7. **Task dependencies** (`dependsOn`, `blockedBy`, `completedBy`)
8. **Conflict detection** when multiple active tasks reference the same artifact
9. **Human intervention** — request review/approval/clarification, pause, resume
10. **Append-only timeline** for audit and portal display

## Session lifecycle

`CREATED` → `STARTING` → `ACTIVE` → (`WAITING` | `BLOCKED`) → `COMPLETED` | `FAILED` | `CANCELLED`

## API

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/collaboration` | `COLLABORATION_READ` |
| GET | `/api/collaboration/config` | `COLLABORATION_READ` |
| GET | `/api/collaboration/{id}` | `COLLABORATION_READ` |
| GET | `/api/collaboration/{id}/timeline` | `COLLABORATION_READ` |
| GET | `/api/collaboration/{id}/participants` | `COLLABORATION_READ` |
| GET | `/api/collaboration/{id}/messages` | `COLLABORATION_READ` |
| POST | `/api/collaboration` | `COLLABORATION_WRITE` |
| POST | `/api/collaboration/{id}/assign` | `COLLABORATION_WRITE` |
| POST | `/api/collaboration/{id}/message` | `COLLABORATION_WRITE` |
| POST | `/api/collaboration/{id}/decision` | `COLLABORATION_WRITE` |
| POST | `/api/collaboration/{id}/pause` | `COLLABORATION_ADMIN` |
| POST | `/api/collaboration/{id}/resume` | `COLLABORATION_ADMIN` |
| POST | `/api/collaboration/{id}/cancel` | `COLLABORATION_ADMIN` |

## Configuration

```yaml
nova:
  collaboration:
    enabled: true
    polling-seconds: 10
    max-messages: 500
```

## Portal

Route `/collaboration` provides Overview, Participants, Timeline, Messages, Tasks, Decisions, Conflicts, and Human Requests views with HTTP polling (default 10 seconds).

## Database

Flyway `V55__collaboration_framework.sql` creates:

- `collaboration_sessions`
- `collaboration_participants`
- `collaboration_tasks`
- `collaboration_messages`
- `collaboration_decisions`
- `collaboration_timeline_events`

Permissions: `COLLABORATION_READ`, `COLLABORATION_WRITE`, `COLLABORATION_ADMIN`.

## Concurrency and consistency

- Optimistic locking via `@Version` on sessions, participants, and tasks
- Concurrent modification returns HTTP 409 `COLLABORATION_CONCURRENT_MODIFICATION` with reload-and-retry guidance
- One active task per participant per session (`ASSIGNED`, `IN_PROGRESS`, `BLOCKED`)
- Participant busy returns HTTP 409 `COLLABORATION_PARTICIPANT_BUSY`
- Message limit enforced per session with HTTP 409 `COLLABORATION_MESSAGE_LIMIT_REACHED`
- Session and task transitions validated centrally; terminal states reject further mutation
- Cross-session and cross-tenant references rejected at service layer

## Audit

All collaboration lifecycle events publish to Audit Center with `AuditSource.COLLABORATION`.

## Constraints

- No Kafka, RabbitMQ, Redis, or WebSockets
- No replacement of orchestration engine
- No Knowledge Base, Memory, Vector DB, or autonomous planning in this phase

See [ADR-0032](adr/ADR-0032-multi-agent-collaboration.md).
