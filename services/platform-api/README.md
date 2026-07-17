# Nova Platform API

Spring Boot control-plane service for identity, organizations, workspaces, projects,
policies, audit records, feedback, and orchestration-facing APIs.

## Stack

- Java 21
- Spring Boot 3.5.x
- Maven Wrapper
- Package root: `ai.nova.platform`

## Boundary

AI provider SDKs and code-execution logic do not belong in this service.

## Prerequisites

- JDK 21
- Maven Wrapper (included) or Maven 3.9+

## Configuration

Configuration is externalized through environment variables. Copy `.env.example`
and export values in your shell, or set them in your IDE run configuration.

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | HTTP port |
| `SPRING_APPLICATION_NAME` | `platform-api` | Application name |
| `LOG_LEVEL_ROOT` | `INFO` | Root log level |
| `LOG_LEVEL_APP` | `INFO` | `ai.nova.platform` log level |
| `SPRING_PROFILES_ACTIVE` | _(empty)_ | Set `json-logs` for structured JSON console logging |

Do not commit real secrets. This foundation does not require database credentials yet.

## Run

```bash
cd services/platform-api
./mvnw spring-boot:run
```

Windows:

```powershell
cd services/platform-api
.\mvnw.cmd spring-boot:run
```

## Test

Unit and Spring integration tests:

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
