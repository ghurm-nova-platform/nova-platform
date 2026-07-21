# Enterprise Dashboard

Sprint 5 Phase 2 Ã¢â‚¬â€ read-only enterprise dashboard aggregating operational signals from existing Platform API domains. The dashboard never mutates business data and does not introduce operational tables beyond optional `dashboard_user_preferences`.

## Capabilities

1. Executive overview counts and KPIs (success rates, average durations)
2. Live pipeline across 14 stages (Planner Ã¢â€ â€™ Rollback)
3. Running deployment executions with provider, stage, progress, and verify status
4. Release lifecycle buckets (published, ready, blocked, policy failures, rollback ready)
5. Environment health buckets (production, staging, qa, dev)
6. Audit feed with filter passthrough to Audit Center search
7. Approval queue (waiting, SLA, expired, blocked)
8. CI observation summary (failed builds, repair requests, queue depth)
9. Rollback planning coverage (planning-only)
10. Cost placeholder section for future billing/LLM estimates

## API

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/dashboard` | `DASHBOARD_READ` |
| GET | `/api/dashboard/config` | `DASHBOARD_READ` |
| GET | `/api/dashboard/overview` | `DASHBOARD_READ` |
| GET | `/api/dashboard/pipeline` | `DASHBOARD_READ` |
| GET | `/api/dashboard/deployments` | `DASHBOARD_READ` |
| GET | `/api/dashboard/releases` | `DASHBOARD_READ` |
| GET | `/api/dashboard/environments` | `DASHBOARD_READ` |
| GET | `/api/dashboard/audit` | `DASHBOARD_READ` (+ audit search via `AUDIT_READ`) |
| GET | `/api/dashboard/approvals` | `DASHBOARD_READ` |
| GET | `/api/dashboard/ci` | `DASHBOARD_READ` |
| GET | `/api/dashboard/rollbacks` | `DASHBOARD_READ` |
| GET | `/api/dashboard/cost` | `DASHBOARD_READ` |
| POST | `/api/dashboard/refresh` | `DASHBOARD_ADMIN` |
| GET | `/api/dashboard/export?format=csv\|xlsx\|pdf&section=...` | `DASHBOARD_READ` |

Optional query parameter: `projectId` scopes all sections to a project.

## Configuration

```yaml
nova:
  dashboard:
    enabled: true
    refresh-rate-seconds: 30
    cache:
      enabled: true
      ttl-seconds: 30
```

Portal polls using `refresh-rate-seconds` (default 30s). Backend cache TTL defaults to 30s.

## Permissions (Flyway V54)

- `DASHBOARD_READ` Ã¢â‚¬â€ `33333333-3333-3333-3333-333333331088`
- `DASHBOARD_ADMIN` Ã¢â‚¬â€ `33333333-3333-3333-3333-333333331089`

Granted to all four platform roles (ORG_ADMIN, ORG_MEMBER, PROJECT_ADMIN, USER).

## Portal

Route `/dashboard` renders the Enterprise Dashboard with Material cards, pipeline/environment charts (ECharts), tables, and HTTP polling.

## Constraints

- Read-only aggregation from existing repositories/services only
- In-memory org-scoped cache with TTL; no Kafka/Redis/WebSockets
- Export: CSV, real XLSX via Apache POI, and valid PDF via PDFBox
- No duplicated agent business logic

See [ADR-0031](adr/ADR-0031-enterprise-dashboard.md).

## Export limitations

- XLSX exports neutralize formula-like user input by prefixing values that begin with =, +, -, or @.
- PDF exports are structurally valid and multi-page, but they currently rely on built-in Helvetica without embedded custom fonts. English text is preserved; complex Arabic shaping is not guaranteed until a distributable Unicode font strategy is added.
