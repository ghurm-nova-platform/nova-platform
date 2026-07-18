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
- [Cursor execution guide](docs/007_CURSOR_EXECUTION_GUIDE.md)
- [CI/CD guide](docs/008_CI_CD_GUIDE.md)
- [Authentication API](docs/009_AUTH_API.md)
- [Organizations and Projects API](docs/010_ORGANIZATIONS_PROJECTS_API.md)
- [Database ER diagram](docs/011_DATABASE_ER.md)
- [Agent Management API](docs/012_AGENT_MANAGEMENT_API.md)
- [Prompt Management API](docs/013_PROMPT_MANAGEMENT_API.md)
- [Prompt Versioning](docs/014_PROMPT_VERSIONING.md)
- [Agent Execution Engine](docs/015_AGENT_EXECUTION.md)
- [Conversation Memory](docs/016_CONVERSATION_MEMORY.md)
- [Tool Registry and Calling](docs/017_TOOL_REGISTRY_AND_CALLING.md)
- [Knowledge Bases and RAG](docs/018_KNOWLEDGE_BASE_AND_RAG.md)
- [AI Model Gateway](docs/019_AI_MODEL_GATEWAY.md)
- [Secure Provider Integration](docs/020_SECURE_PROVIDER_INTEGRATION.md)
- [Model Catalog](docs/021_MODEL_CATALOG.md)
- [Multi-Agent Orchestration Foundation](docs/022_MULTI_AGENT_ORCHESTRATION_FOUNDATION.md)
- [Planner Agent](docs/023_PLANNER_AGENT.md)
- [Coding Agent](docs/024_CODING_AGENT.md)
- [Architecture Decision Records](docs/adr/README.md)

## Engineering principles

- Human control over high-risk operations
- Read-only agents by default
- Provider-neutral AI contracts
- Event-driven workflows with durable audit trails
- Isolated and resource-limited code execution
- Arabic RTL and English LTR support
- Modular monorepo first; service extraction only when justified
- Browser → Platform API → Agent Runtime (never browser → Agent Runtime)

## Current status

Sprint 1 Phase 10 — secure provider credentials and OpenAI / Azure OpenAI adapters on Platform API.
