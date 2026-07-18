# Coding Agent

Sprint 2 Phase 3 — Coding Agent is the first production execution agent. It receives one orchestration task and generates structured source artifacts.

## Purpose

Given an orchestration `taskId`, the Coding Agent:

1. Loads the task, run objective, and upstream dependency summaries
2. Builds a coding prompt (`CodingPromptBuilder`) with languages, conventions, org/project settings, and output schema
3. Calls existing `AgentRuntimeClient` / Model Gateway **outside** any DB write transaction
4. Parses JSON into artifacts
5. Validates paths, languages, types, and content (`CODING_*` codes)
6. Persists artifacts via `ArtifactStorageService` (SHA-256 + metadata)

The Coding Agent never executes shell, git, docker, browser, MCP, or terminal commands, and never mutates repositories. Artifacts are stored for later Git commit phases.

## APIs

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/coding/generate` | `CODING_GENERATE` |
| GET | `/api/coding/artifacts/{taskId}` | `CODING_READ` |

## Domain objects

- `CodeGenerationRequest` / `CodingResult`
- `GeneratedArtifact`, `ArtifactType`, `ArtifactLanguage`
- `generated_artifacts` (Flyway **V35**)

## Artifact types

`SOURCE_FILE`, `PATCH`, `TEST`, `DOCUMENTATION`, `CONFIGURATION`, `SQL_MIGRATION`, `README`

## Languages

Java, Kotlin, TypeScript, JavaScript, Angular, HTML, CSS, SCSS, SQL, Oracle SQL, PostgreSQL, MySQL, Python, Go, C#, Markdown, JSON, YAML, XML, Shell

## Validation error codes

`CODING_EMPTY_ARTIFACTS`, `CODING_EMPTY_CONTENT`, `CODING_DUPLICATE_PATH`, `CODING_INVALID_LANGUAGE`, `CODING_INVALID_ARTIFACT_TYPE`, `CODING_PATH_TRAVERSAL`, `CODING_ABSOLUTE_PATH`, `CODING_BINARY_CONTENT`, `CODING_INVALID_OUTPUT`, `CODING_TASK_NOT_FOUND`, …

## Portal

Route `/coding` — Coding Agent: generate/load artifacts, search/filter, syntax-style preview, lightweight diff (no Monaco), copy, download.

## Related

- ADR-0014
- Planner Agent (`023`, ADR-0013)
- Orchestration foundation (`022`, ADR-0012)
