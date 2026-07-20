# Sprint 4 Phase 6 — Enterprise Audit Center

Read-only portal surface for the append-only enterprise audit trail.

## Scope

- Route `/audit` with search/list/detail views
- Permission gate: `AUDIT_READ`
- No write APIs exposed in the portal

## Safety

> Enterprise Audit Center is append-only and read-only in the portal. It never modifies business data.
