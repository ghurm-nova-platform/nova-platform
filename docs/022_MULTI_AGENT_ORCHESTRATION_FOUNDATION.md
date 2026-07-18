# Multi-Agent Orchestration Foundation

Sprint 2 Phase 1 — durable multi-agent orchestration engine owned by Platform API.

## Purpose

Provide a production-grade foundation for multi-agent workflow runs: directed graphs, dependency-aware scheduling, claim leases, retries, cancellation, and stale-safe execution boundaries. Specialized Planner/Coding/Review agents are **out of scope**.

## Boundary

```text
Portal / API
  → OrchestrationRunService / TaskService
  → Scheduler (claim) → OrchestrationExecutionService
      TX1 (claim → RUNNING + attempt)
      → AgentRuntimeClient / Model Gateway (outside DB TX)
      TX2 (version + cancel checks → apply or STALE)
  → Durable tables (runs, tasks, deps, attempts, events)
```

## Domain model

| Entity | Role |
|--------|------|
| `agent_orchestration_runs` | Workflow execution + event sequence counter |
| `agent_orchestration_tasks` | Units of work with claim lease + retry |
| `agent_task_dependencies` | SUCCESS / COMPLETION edges |
| `agent_task_attempts` | Append-only attempt history |
| `agent_orchestration_events` | Ordered timeline (`run_id`, `event_sequence`) |

Migrations: **V29–V32**.

## Run lifecycle

`DRAFT` → `READY` → `RUNNING` ⇄ `WAITING` → terminal (`SUCCEEDED` | `PARTIALLY_SUCCEEDED` | `FAILED` | `CANCELLED` | `TIMED_OUT`) → `ARCHIVED`

`CANCEL_REQUESTED` is monotonic and wins over late provider results.

## Task lifecycle

`DRAFT` → `BLOCKED` / `READY` → `CLAIMED` → `RUNNING` → `SUCCEEDED` | `FAILED` | `RETRY_WAIT` | `TIMED_OUT` | `CANCELLED` | `SKIPPED`

Atomic claim uses compare-and-set on status + version. Expired `CLAIMED` (never started) may be reclaimed; `RUNNING` is never reclaimed by lease alone.

## Dependency semantics

- **SUCCESS** — successor eligible only if predecessor `SUCCEEDED`; otherwise skip / apply failure policy.
- **COMPLETION** — successor eligible after any terminal predecessor state.

Cycles, self-edges, and cross-run edges are rejected at READY.

## Scheduling / claim

Configurable poller (`nova.orchestration.*`). Disabled in tests (`enabled: false`). Safe for multi-node via DB CAS updates — not JVM locks.

## TX1 / external / TX2

1. **TX1:** validate, `CLAIMED`→`RUNNING`, create STARTED attempt, snapshot versions.
2. **External:** `AgentRuntimeClient.execute` / Model Gateway — **no open DB transaction**.
3. **TX2:** reload; if cancelled, version mismatch, or superseded attempt → ignore output, emit `TASK_STALE_RESULT_IGNORED`; else apply result and finalize.

## Idempotency

- Unique `(run_id, idempotency_key)` and `(run_id, task_key)`
- Unique `(task_id, attempt_number)`
- Claim CAS prevents double dispatch
- TX2 rejects stale completions

## Retry

Central `TaskRetryPolicy`: exponential backoff with cap; only transient classifications; durable `next_attempt_at`; no sleep inside DB TX.

## Cancellation / deadlines

`POST .../cancel` is idempotent. Run deadline → `TIMED_OUT`. Late successes after cancel never revive tasks.

## Failure policies / finalization

| Policy | Behavior |
|--------|----------|
| `FAIL_FAST` | Unsuccessful required branch fails the run |
| `CONTINUE_INDEPENDENT` | Unrelated branches continue |
| `BEST_EFFORT` | Mixed terminals → `PARTIALLY_SUCCEEDED` |

`WAITING` when retries or human approval pending and nothing actively running.

## JSON input references

Constrained resolver only:

```json
{ "fromTask": "research-task", "path": "$.summary" }
```

No SpEL/JS/SQL. Bounded size. Errors: `TASK_INPUT_*`.

## Security

Org + project scoping on every repository query. RBAC: `ORCHESTRATION_*` (V32). No secrets in events/outputs/logs. Model references resolve via catalog with existing CHAT/tool capability gates.

## API summary

`/api/orchestration-runs` — CRUD, ready, start, cancel, archive, tasks, dependencies, graph, events, attempts.

## Portal

`/orchestration-runs` — list, form, detail (polling), list-based graph builder.

## Known limitations

- No specialized domain agents, recursive spawning, Kafka/Temporal, WebSockets, full human-approval UI, streaming, or billing.
- TRANSFORM / AGGREGATION / HUMAN_APPROVAL lifecycle only; dispatch fails with `TASK_TYPE_UNSUPPORTED` where not implemented.
- Scheduler is in-process; replaceable with an external queue later without rewriting the domain.

## Future migration path

Keep domain tables as source of truth. Swap `OrchestrationScheduler` / claim worker for an external durable queue or workflow engine behind the same TX1/TX2 and state machine contracts.

See [ADR-0012](adr/ADR-0012-durable-agent-orchestration.md).
