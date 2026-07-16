# Sprint 0 Backlog

**Sprint duration:** 2 weeks  
**Sprint goal:** Establish a working, testable monorepo foundation for Nova Platform.

## P0 — Must complete

### S0-01 Repository structure

Create the agreed monorepo layout for applications, services, shared packages, infrastructure, and documentation.

**Acceptance criteria**

- Standard directories exist with ownership documented.
- Root README links to architecture and setup documentation.
- Empty modules contain a short README rather than placeholder source code.

### S0-02 Architecture decision process

Create an ADR template and record initial decisions for monorepo architecture, core stack, event broker, and database.

**Acceptance criteria**

- ADR directory and template are available.
- Initial ADRs include status, context, decision, consequences, and alternatives.

### S0-03 Local development environment

Provide Docker Compose for PostgreSQL, Redis, RabbitMQ, and object storage.

**Acceptance criteria**

- One documented command starts dependencies.
- Services include health checks and persistent development volumes.
- No committed real secrets.

### S0-04 Backend skeleton

Create the Spring Boot platform API foundation.

**Acceptance criteria**

- Application starts.
- Health endpoint succeeds.
- Test suite runs.
- Configuration uses environment variables.
- Error response contract is documented.

### S0-05 Frontend skeleton

Create the Angular portal foundation.

**Acceptance criteria**

- Application starts and builds.
- Initial navigation includes Dashboard, Projects, Agents, Feedback, and Settings.
- Lint and tests execute in CI.
- RTL readiness is included in layout decisions.

### S0-06 AI service skeleton

Create a Python service exposing health and agent-registry endpoints.

**Acceptance criteria**

- Typed configuration and API schemas.
- Test command documented.
- No direct dependency on a specific model provider in core contracts.

### S0-07 CI baseline

Configure GitHub Actions for documentation checks, frontend build, backend tests, Python tests, secret scanning, and dependency review where supported.

**Acceptance criteria**

- Pull requests receive automated checks.
- Failed checks block readiness for merge by policy recommendation.
- Build steps are cached safely.

### S0-08 Security baseline

Define secrets handling, branch protections, dependency policy, and agent permission defaults.

**Acceptance criteria**

- Security policy document exists.
- Agents are read-only by default.
- Protected-branch workflow is documented.

## P1 — Should complete

- Event contract draft
- API error and pagination standard
- OpenTelemetry instrumentation plan
- Contribution guide
- Pull request template
- Issue templates for bug, feature, and architecture decision

## Definition of Done

A Sprint 0 item is done only when documentation, automated tests, and relevant setup instructions are committed and verified. Generated code without a successful build or test result is not considered complete.
