# ADR-0013: Planner Agent and dynamic workflow generation

- Status: Accepted
- Date: 2026-07-18

## Context

Nova needs a Planner that turns natural-language objectives into validated multi-agent DAGs before any execution. Specialized coding/review agents and recursive planners are not ready yet, but the planning boundary must exist now and remain compatible with future agent roles.

## Decision

1. Implement Planner as a Platform API module that calls the existing Agent Runtime / Model Gateway for structured JSON plans only.
2. Persist reusable `planner_templates` (V34) and RBAC (`PLANNER_*`).
3. Validate plans independently of orchestration READY validation, then import into DRAFT runs via `PlannerImportService`.
4. Do not execute workflows, tools, shell, git, MCP, or browser from the Planner.
5. Keep agentRole as an open vocabulary for future specialized agents.

## Consequences

- Operators can preview cost/risk/DAG before starting runs.
- Import creates draft orchestration graphs without auto-start.
- Plan quality depends on the underlying model; invalid JSON fails with stable codes.
- Future agents plug in via roles without rewriting Planner APIs.
