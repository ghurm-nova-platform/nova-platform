# ADR-0001: Start with a modular monorepo

- Status: Accepted
- Date: 2026-07-16
- Owners: Architecture Team
- Supersedes: None

## Context

Nova Platform spans a web portal, web IDE, platform APIs, an agent runtime, sandbox workers, shared SDKs, and infrastructure. The initial team is small and needs rapid, consistent delivery without the operational burden of many repositories and independently deployed microservices.

## Decision

Use one GitHub repository with explicit application, service, package, infrastructure, and documentation boundaries. Start with a modular control plane and extract independently deployable services only when scaling, security isolation, ownership, or release cadence justifies the cost.

## Alternatives considered

- Multiple repositories from day one: rejected because it increases coordination and versioning overhead.
- A single unstructured application: rejected because it creates hidden coupling and blocks future extraction.
- Dozens of microservices immediately: rejected because operational complexity would exceed current product needs.

## Consequences

### Positive

- Atomic cross-module changes and simpler developer setup.
- Shared quality controls and architecture documentation.
- Lower initial CI/CD and operations cost.

### Negative and risks

- Boundaries may erode without ownership rules and dependency checks.
- CI duration can increase as the repository grows.

## Security and privacy impact

Sensitive execution remains separated in sandbox workers even while source code is stored in the same repository.

## Operational and cost impact

Lower initial infrastructure and release-management cost. Selective builds and path-based CI will be required later.

## Follow-up actions

- [ ] Add module ownership documentation.
- [ ] Add dependency-boundary checks.
- [ ] Review extraction candidates at every major release.
