# Planner Agent

Sprint 2 Phase 2 — Planner Agent produces validated multi-agent execution plans without executing them.

## Purpose

Given a user objective, the Planner:

1. Builds a planning prompt (org/project agents, tools, schema, constraints)
2. Calls existing `AgentRuntimeClient` / Model Gateway
3. Parses structured JSON into an `ExecutionPlan`
4. Validates the DAG (keys, cycles, roles, modes)
5. Estimates complexity, tokens, duration, cost, and risk
Optional imports the plan into a **DRAFT** orchestration run via `PlannerPlanImporter` (single `@Transactional` boundary for run + tasks + dependencies). External planning calls stay outside that transaction.

The Planner never starts orchestration, never invokes tools, shell, git, browser, or MCP.

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/planner/plan` | `PLANNER_PLAN` |
| POST | `/api/planner/plan-and-create` | `PLANNER_PLAN` + `PLANNER_IMPORT` |
| POST | `/api/planner/import` | `PLANNER_IMPORT` |
| GET | `/api/planner/templates?projectId=` | `PLANNER_TEMPLATE_READ` |

## Domain objects

- `PlannerRequest` / `PlannerResponse`
- `ExecutionPlan`, `ExecutionTaskDefinition`, `ExecutionDependency`, `ExecutionEstimate`
- `planner_templates` (Flyway **V34**)

## Validation error codes

`PLANNER_EMPTY_TASKS`, `PLANNER_DUPLICATE_TASK_KEY`, `PLANNER_GRAPH_CYCLE`, `PLANNER_UNKNOWN_DEPENDENCY`, `PLANNER_SELF_DEPENDENCY`, `PLANNER_AGENT_ROLE_REQUIRED`, `PLANNER_AGENT_ROLE_INVALID`, `PLANNER_OBJECTIVE_REQUIRED`, `PLANNER_EXECUTION_MODE_INVALID`, `PLANNER_FAILURE_POLICY_INVALID`, `PLANNER_INVALID_OUTPUT`, …

## Agent roles (future-compatible)

planner, research, coding, review, testing, documentation, security, devops, database, ui, backend, frontend, architecture, human, transform, aggregation

## Portal

Route `/planner` — AI Planner: objective → generate → DAG preview (zoom/pan) → estimates → edit JSON → create draft run.

## Related

- ADR-0013
- Orchestration foundation (`022`, ADR-0012)
