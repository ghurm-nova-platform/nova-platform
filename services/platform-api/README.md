# Nova Platform API

Spring Boot control-plane service for identity, organizations, workspaces, projects,
policies, audit records, feedback, and orchestration-facing APIs.

## Stack

- Java 21
- Spring Boot 3.5.x
- Spring Security (stateless JWT + BCrypt + RBAC)
- Spring Data JPA + Flyway + PostgreSQL
- Maven Wrapper
- Package root: `ai.nova.platform`

## Boundary

```text
Browser → Platform API → Agent Runtime (server-to-server only)
```

AI provider SDKs and code-execution logic do not belong in this service.
The browser must never call Agent Runtime directly.

## Prerequisites

- JDK 21
- Maven Wrapper (included) or Maven 3.9+
- PostgreSQL 16 for local runs (`infrastructure/local` Compose service)

## Configuration

Configuration is externalized through environment variables. Copy `.env.example`
and export values in your shell, or set them in your IDE run configuration.

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | HTTP port |
| `SPRING_APPLICATION_NAME` | `platform-api` | Application name |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/nova` | JDBC URL |
| `DATABASE_USERNAME` | `nova` | DB user |
| `DATABASE_PASSWORD` | `nova_local_only` | DB password (local only) |
| `JWT_SECRET` | _(required)_ | HMAC secret, >= 32 bytes |
| `JWT_ISSUER` | `nova-platform` | JWT issuer claim |
| `JWT_ACCESS_TTL` | `PT15M` | Access token TTL |
| `JWT_REFRESH_TTL` | `P7D` | Refresh token TTL |
| `LOG_LEVEL_ROOT` | `INFO` | Root log level |
| `LOG_LEVEL_APP` | `INFO` | `ai.nova.platform` log level |
| `SPRING_PROFILES_ACTIVE` | _(empty)_ | Set `json-logs` for structured JSON console logging |
| `NOVA_SECRET_MASTER_KEY` | _(required for vault)_ | Base64-encoded 32-byte AES-256-GCM master key |

Do not commit real secrets. Generate `JWT_SECRET` per environment (for example `openssl rand -base64 48`).
Generate `NOVA_SECRET_MASTER_KEY` with `openssl rand -base64 32` where provider secret vault is used.

## Package layout

```text
ai.nova.platform
  auth/            login, refresh, logout, /me
  security/        JWT filter, SecurityConfig, JwtService
  user/            UserAccount entity
  organization/    Organization entity
  role/            Role entity + permissions join
  permission/      Permission entity
  web/             health, correlation, errors
```

## Authentication

See [`docs/009_AUTH_API.md`](../../docs/009_AUTH_API.md).

## Organizations and projects

See [`docs/010_ORGANIZATIONS_PROJECTS_API.md`](../../docs/010_ORGANIZATIONS_PROJECTS_API.md)
and [`docs/011_DATABASE_ER.md`](../../docs/011_DATABASE_ER.md).

## Agents

See [`docs/012_AGENT_MANAGEMENT_API.md`](../../docs/012_AGENT_MANAGEMENT_API.md).

## Prompts

See [`docs/013_PROMPT_MANAGEMENT_API.md`](../../docs/013_PROMPT_MANAGEMENT_API.md)
and [`docs/014_PROMPT_VERSIONING.md`](../../docs/014_PROMPT_VERSIONING.md).

## Agent execution

See [`docs/015_AGENT_EXECUTION.md`](../../docs/015_AGENT_EXECUTION.md).

## Conversations

See [`docs/016_CONVERSATION_MEMORY.md`](../../docs/016_CONVERSATION_MEMORY.md).

## Tools

See [`docs/017_TOOL_REGISTRY_AND_CALLING.md`](../../docs/017_TOOL_REGISTRY_AND_CALLING.md).

## Knowledge bases and RAG

See [`docs/018_KNOWLEDGE_BASE_AND_RAG.md`](../../docs/018_KNOWLEDGE_BASE_AND_RAG.md).

Migrations: `V16`–`V19`. Embedding provider allowlist starts with `DETERMINISTIC_LOCAL`.
PDF extraction is deferred (`nova.knowledge.pdf-enabled=false`).

## AI model gateway

See [`docs/019_AI_MODEL_GATEWAY.md`](../../docs/019_AI_MODEL_GATEWAY.md).

Migrations: `V20`–`V22`. Gateway routing, invocations, and usage.

## Secure provider integration

See [`docs/020_SECURE_PROVIDER_INTEGRATION.md`](../../docs/020_SECURE_PROVIDER_INTEGRATION.md).

Migrations: `V23`–`V25`. Executable adapters: `DETERMINISTIC_LOCAL`, `OPENAI`, `AZURE_OPENAI`.
Credential references: `vault:provider-secret:<uuid>` or `env:NOVA_PROVIDER_*` — never plaintext secrets in persistence or API responses after create/rotate.

## Model catalog

See [`docs/021_MODEL_CATALOG.md`](../../docs/021_MODEL_CATALOG.md).

Migrations: `V26`–`V28`. Catalog CRUD, capabilities, aliases, provider sync, optional gateway `modelReference`.

## Multi-agent orchestration

See [`docs/022_MULTI_AGENT_ORCHESTRATION_FOUNDATION.md`](../../docs/022_MULTI_AGENT_ORCHESTRATION_FOUNDATION.md).

## Planner Agent

See [`docs/023_PLANNER_AGENT.md`](../../docs/023_PLANNER_AGENT.md) and ADR-0013.

## Coding Agent

See [`docs/024_CODING_AGENT.md`](../../docs/024_CODING_AGENT.md) and ADR-0014.

## Review Agent

See [`docs/025_REVIEW_AGENT.md`](../../docs/025_REVIEW_AGENT.md) and ADR-0015.

Migrations: `V29`–`V32`. Durable runs, tasks, dependencies, attempts, events; claim leases; TX1/external/TX2 execution.
Migration `V34` planner templates; `V35` generated artifacts; `V36` review results/findings.

Local demo user (Flyway seed, local only):

- `admin@nova.local` / `ChangeMe123!`
- Roles: `ORG_ADMIN`, `PROJECT_ADMIN`
- Includes agent, prompt, and execution permissions
- Seeded ACTIVE demo agent linked to a published demo prompt

## Run

```bash
cd services/platform-api
export JWT_SECRET="$(openssl rand -base64 48)"
./mvnw spring-boot:run
```

Windows (PowerShell):

```powershell
cd services/platform-api
$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
.\mvnw.cmd spring-boot:run
```

## Test

Unit and Spring Security integration tests use an in-memory H2 database:

```bash
./mvnw test
```

Full verify lifecycle (unit + failsafe integration tests):

```bash
./mvnw verify
```

## Build

```bash
./mvnw -DskipTests package
```

Runnable JAR:

```text
target/platform-api-0.1.0-SNAPSHOT.jar
```

## Health endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/health` | Application health (JSON) |
| `GET /actuator/health` | Actuator health probe |

Actuator exposure is limited to `health`. Sensitive endpoints such as `env`,
`beans`, and `heapdump` are not publicly exposed.

Correlation IDs:

- Incoming `X-Correlation-Id` is accepted when present.
- Otherwise a UUID is generated.
- The value is returned on the response and included in log MDC as `correlationId`.

## API error response contract

All handled errors return JSON in this shape:

```json
{
  "timestamp": "2026-07-16T20:00:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "RESOURCE_NOT_FOUND",
  "message": "The requested resource was not found",
  "path": "/api/v1/missing",
  "correlationId": "uuid",
  "details": [
    { "field": "name", "message": "must not be blank" }
  ]
}
```

`details` is present only for validation failures.
