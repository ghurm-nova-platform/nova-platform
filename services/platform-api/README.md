# Nova Platform API

Spring Boot service for identity, organizations, workspaces, projects, policies, audit records, feedback, and orchestration-facing APIs.

## Baseline

- Java 21
- Spring Boot 3.x
- PostgreSQL
- REST and asynchronous domain events
- Correlation IDs and structured logs

## Boundary

AI provider SDKs and code-execution logic do not belong in this service.
