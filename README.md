# Nova AI Platform

> AI-native software engineering platform where governed agent teams help plan, build, test, review, and operate software.

## Product direction

Nova combines a web administration portal, browser IDE, provider-neutral agent runtime, isolated execution sandboxes, project knowledge, feedback automation, and enterprise governance.

## Repository structure

```text
apps/
  portal/            Angular administration portal
  web-ide/           Browser development workspace
services/
  platform-api/      Spring Boot control-plane API
  agent-runtime/     Python agent orchestration runtime
  sandbox-worker/    Isolated code execution worker
packages/            Shared contracts, UI, agent SDK, and model SDK
infrastructure/      Local and deployment infrastructure
docs/                Product, architecture, security, and delivery documents
```

## Start local dependencies

```bash
cd infrastructure/local
cp .env.example .env
docker compose up -d
```

See [`infrastructure/local/README.md`](infrastructure/local/README.md) for operations and reset instructions.

## Foundation documents

- [Project charter](docs/000_PROJECT_CHARTER.md)
- [Product vision](docs/001_PRODUCT_VISION.md)
- [System architecture](docs/002_SYSTEM_ARCHITECTURE.md)
- [Agent runtime](docs/003_AGENT_RUNTIME.md)
- [Feedback automation](docs/004_FEEDBACK_AUTOMATION.md)
- [Roadmap](docs/005_ROADMAP.md)
- [Sprint 0 backlog](docs/006_SPRINT_0_BACKLOG.md)
- [Architecture Decision Records](docs/adr/README.md)

## Engineering principles

- Human control over high-risk operations
- Read-only agents by default
- Provider-neutral AI contracts
- Event-driven workflows with durable audit trails
- Isolated and resource-limited code execution
- Arabic RTL and English LTR support
- Modular monorepo first; service extraction only when justified

## Current status

Sprint 0 — repository, architecture, security, local infrastructure, and application skeletons.
