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
- [Sprint 3 backlog](docs/025_SPRINT_3_BACKLOG.md)
- [Review Agent](docs/026_REVIEW_AGENT.md)
- [Testing Agent](docs/027_TESTING_AGENT.md)
- [Patch Agent](docs/028_PATCH_AGENT.md)
- [Git Integration Agent](docs/029_GIT_INTEGRATION_AGENT.md)
- [Pull Request Agent](docs/030_PULL_REQUEST_AGENT.md)
- [CI Observation Agent](docs/031_CI_OBSERVATION_AGENT.md)
- [Repair Agent](docs/032_REPAIR_AGENT.md)
- [Approval Gate](docs/033_APPROVAL_GATE.md)
- [Merge Agent](docs/034_MERGE_AGENT.md)
- [Release Manager](docs/035_RELEASE_MANAGER.md)
- [Deployment Observation](docs/036_DEPLOYMENT_OBSERVATION.md)
- [Deployment Execution Engine](docs/041_DEPLOYMENT_EXECUTION.md)
- [Rollback Manager](docs/037_ROLLBACK_MANAGER.md)
- [Release Policies](docs/038_RELEASE_POLICIES.md)
- [Environment Management](docs/039_ENVIRONMENT_MANAGEMENT.md)
- [Enterprise Audit Center](docs/040_AUDIT_CENTER.md)
- [Enterprise Dashboard](docs/042_ENTERPRISE_DASHBOARD.md)
- [Multi-Agent Collaboration](docs/043_MULTI_AGENT_COLLABORATION.md)
- [Knowledge and Memory Engine](docs/044_KNOWLEDGE_ENGINE.md)
- [Automated PR Review Engine](docs/045_PR_REVIEW_ENGINE.md)
- [Enterprise Identity](docs/046_ENTERPRISE_IDENTITY.md)
- [Enterprise Identity OpenAPI](docs/openapi/identity-openapi.yaml)
- [Local LLM Runtime](docs/047_LOCAL_LLM_RUNTIME.md)
- [Local LLM OpenAPI](docs/openapi/llm-openapi.yaml)
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

Sprint 6 Phase 5 — Local LLM Runtime provides gateway-only local inference (Ollama / llama.cpp / vLLM / deterministic), model lifecycle, conversations, and prompts via `/api/llm` and portal `/llm` (no direct provider calls from other modules). Sprint 6 Phase 4 — Enterprise Identity wraps the existing JWT foundation with provider registry (SAML/OIDC/LDAP/AD/OAuth2/local), session management, login history, MFA enrollment (TOTP), and optional SCIM user listing via `/api/identity` and portal `/identity`. Sprint 6 Phase 3 — Automated PR Review Engine analyzes supplied PR diffs with rule-based heuristics, six-category risk scores, knowledge linkage, and recommendations via `/api/pr-review` and portal `/pr-review` (no LLM, auto-merge, commit, push, fix, or GitHub writes). Sprint 6 Phase 2 — Knowledge and Memory Engine provides structured project documents with keyword search, relations, import/export, and agent memory surfacing via `/api/knowledge` (parallel to the RAG stack; no vectors/embeddings). Sprint 6 Phase 1 delivered Multi-Agent Collaboration; Sprint 5 delivered Deployment Execution and Enterprise Dashboard on `main`.
