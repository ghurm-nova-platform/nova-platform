# ADR-0031: Enterprise Dashboard (read-only aggregation)

## Status

Accepted

## Context

Nova Platform operators need a single-pane view across orchestration, release, deployment, environment, audit, approval, CI, and rollback domains. Prior sprints delivered domain-specific APIs and portal pages but no unified executive dashboard.

Requirements:

- Read-only aggregation from existing repositories
- No new operational/business tables except optional `dashboard_user_preferences`
- No duplicated business logic
- No Kafka, Redis, WebSockets, Grafana, Prometheus, OpenTelemetry, billing, or ML infrastructure
- In-memory cache with TTL (30s default) and HTTP polling only
- CSV/Excel/PDF export of snapshot sections

## Decision

Implement `ai.nova.platform.dashboard` on Platform API with:

1. **DashboardAggregationService** — injects existing repositories only; scopes by `AuthenticatedUser.getOrganizationId()` with optional `projectId` filter
2. **DashboardMetricsService** — derives KPIs from aggregated counts/durations (0 placeholders when no data)
3. **DashboardCacheService** — org-scoped `ConcurrentHashMap` with configurable TTL
4. **DashboardExportService** — CSV, Excel-compatible export (CSV bytes + xlsx mime without POI), minimal PDF writer
5. **DashboardController** — section endpoints + admin refresh + export
6. **Portal** — `/dashboard` with ECharts widgets and polling interval from API config
7. **Flyway V54** — `DASHBOARD_READ`, `DASHBOARD_ADMIN`, optional `dashboard_user_preferences`

Pipeline stages are fixed at 14 codes (Planner through Rollback). Task keys and sequence order map orchestration tasks to stages without new pipeline tables.

## Consequences

### Positive

- Operators get unified visibility without new data stores or event buses
- Cache reduces load on read-heavy aggregation queries
- Permissions follow established four-role grant pattern

### Negative

- Aggregation loads bounded recent rows from multiple repositories (200-item cap per domain); not a full analytics warehouse
- Cost section remains placeholder until billing integration
- Audit subsection delegates to Audit Center and requires `AUDIT_READ`

### Alternatives considered

- **Real-time WebSocket push** — rejected (explicit constraint)
- **Materialized dashboard tables** — rejected (would duplicate business state)
- **Grafana/Prometheus** — rejected (explicit constraint)

## References

- [Enterprise Dashboard](../042_ENTERPRISE_DASHBOARD.md)
- [Deployment Execution Engine](../041_DEPLOYMENT_EXECUTION.md)
- [Enterprise Audit Center](../040_AUDIT_CENTER.md)
