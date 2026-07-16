# Cursor Execution Guide

## Purpose

This guide defines how Cursor is used as an implementation assistant for Nova Platform while GitHub remains the system of record.

## When to start

Cursor execution starts immediately after the developer clones the repository and checks out the active implementation branch.

Current branch:

```bash
sprint-0/foundation
```

## Working model

- GitHub Issues define the approved work.
- Architecture documents and ADRs define the constraints.
- Cursor implements one issue at a time.
- Every material change is committed to a feature branch.
- Pull requests are reviewed before merge.
- Cursor must not invent features outside the selected issue.

## Initial setup

```bash
git clone https://github.com/ghurm-nova-platform/nova-platform.git
cd nova-platform
git fetch --all
git checkout sprint-0/foundation
```

Open the repository root in Cursor.

## Required reading before implementation

Cursor must read these files before changing code:

1. `docs/000_PROJECT_CHARTER.md`
2. `docs/001_PRODUCT_VISION.md`
3. `docs/002_SYSTEM_ARCHITECTURE.md`
4. `docs/003_AGENT_RUNTIME.md`
5. `docs/004_FEEDBACK_AUTOMATION.md`
6. `docs/006_SPRINT_0_BACKLOG.md`
7. `docs/adr/0001-modular-monorepo.md`

## First implementation task

Start with GitHub Issue #4: Create platform API skeleton.

Create a dedicated branch:

```bash
git checkout -b feature/s0-platform-api
```

The implementation must remain inside:

```text
services/platform-api
```

## Cursor prompt for Issue #4

```text
You are implementing GitHub Issue #4 for Nova Platform.

Read all project documents under /docs and the ADRs before modifying files.

Goal:
Create the Spring Boot platform API foundation inside services/platform-api.

Required constraints:
- Java 21.
- Current stable Spring Boot 3.x version compatible with Java 21.
- Maven Wrapper included.
- Package root: ai.nova.platform.
- Health endpoint.
- Unit and integration test foundation.
- Externalized configuration through environment variables.
- Structured JSON-ready logging and correlation ID foundation.
- Standard API error response contract.
- Actuator health enabled but sensitive endpoints not publicly exposed.
- No business-domain implementation beyond the foundation.
- No secrets committed.
- Add a README with run, test, build, and configuration commands.

Before writing code, produce a concise implementation plan and list the files you will create. Then implement the plan. Run all available tests and build commands. Fix failures. At the end, summarize changed files, commands executed, results, risks, and remaining work.
```

## Definition of done

- The selected issue acceptance criteria are met.
- Build and tests pass locally.
- No unrelated files are modified.
- Documentation is updated.
- A clear commit message is used.
- Changes are pushed and a pull request is opened.

## Commit convention

```text
feat(platform-api): create Spring Boot service foundation
```

## Safety rules

Cursor must not:

- write directly to `main`;
- merge pull requests;
- commit credentials or tokens;
- remove security controls;
- execute destructive database or system commands;
- modify architecture decisions without a new ADR;
- implement multiple issues in one branch unless explicitly approved.

## Review cycle

1. Cursor implements the issue.
2. Developer pushes the branch.
3. GitHub Actions validates the change.
4. Architecture and code review are performed.
5. Cursor fixes review comments on the same branch.
6. Pull request is merged only after approval.
