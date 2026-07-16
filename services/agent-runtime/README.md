# Nova Agent Runtime

Provider-neutral Python service that registers agents, plans and coordinates workflows,
invokes approved tools, tracks execution state, and emits auditable events.

## Stack

- Python 3.12
- FastAPI + Pydantic v2
- Poetry
- Pytest / Ruff / Mypy
- Clean Architecture (`api` / `application` / `domain` / `infrastructure` / `shared`)

## Safety

Agents are read-only by default. File writes, terminal commands, Git pushes, deployments,
and database changes require policy evaluation and may require human approval.

No commercial AI provider SDK is imported in core contracts. Model access goes through
`ModelProviderPort` only.

## Layout

```text
src/agent_runtime/
  api/             FastAPI routes, DTOs, middleware, errors
  application/     Use-case services and composition root
  domain/          Entities, enums, ports
  infrastructure/  In-memory adapters (registry, workflow, events, ...)
  shared/          Config, logging, exceptions
tests/
```

## Prerequisites

- Python 3.12
- Poetry 2.x

## Configuration

```bash
cp .env.example .env
```

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8090` | HTTP port |
| `LOG_LEVEL` | `INFO` | Log level |
| `LOG_JSON` | `false` | Structured JSON logs |
| `DEFAULT_AGENT_READONLY` | `true` | Default agent posture |

## Install

```bash
cd services/agent-runtime
poetry install
```

## Run

```bash
poetry run uvicorn agent_runtime.main:create_app --factory --reload --port 8090
```

OpenAPI docs: http://localhost:8090/docs

## Test

```bash
poetry run pytest
```

## Lint and types

```bash
poetry run ruff check src tests
poetry run mypy
```

## Docker

```bash
docker compose up --build
```

## REST endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Health probe |
| `GET` | `/agents` | List agents |
| `POST` | `/agents/register` | Register agent |
| `POST` | `/agents/execute` | Execute agent (policy-gated) |
| `GET` | `/workflows` | List workflows |
| `POST` | `/workflows` | Create workflow |
| `GET` | `/metrics` | Runtime metrics |

## Modules

| Module | Responsibility |
|--------|----------------|
| Agent Registry | Register and discover agents |
| Workflow Engine | Durable workflow state machine |
| Planner | Goal → structured plan |
| Execution Engine | Policy + plan + events |
| Event Bus | Runtime domain events |
| Permission Engine | Least-privilege evaluation |
| Memory Interface | Session/project memory port |
| Knowledge Interface | Knowledge retrieval port |
| Tool Interface | Controlled tool gateway |
| Scheduler | Deferred job recording |
| Metrics | Operational counters |
| Logging | Structured logs + correlation ID |
