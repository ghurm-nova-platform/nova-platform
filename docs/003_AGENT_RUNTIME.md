# Agent Runtime Specification — Draft 0.1

## Purpose

The Agent Runtime is the controlled execution environment for all Nova agents. It coordinates agents, tools, permissions, workflow state, project context, budgets, retries, and audit records.

## Agent definition

Each registered agent must declare:

- Unique identifier and version
- Display name and description
- Supported capabilities
- Accepted input schema
- Output schema
- Required tools
- Required permissions
- Supported model classes
- Maximum execution time
- Retry policy
- Cost and token limits
- Data classification limits
- Events consumed and emitted
- Evaluation rules

## Initial agent teams

### Product intelligence team

- Feedback Intake Agent
- Feedback Classification Agent
- Duplicate Detection Agent
- Severity and Priority Agent
- Routing Agent
- Product Analyst Agent

### Engineering team

- Planner Agent
- Architecture Agent
- Code Search Agent
- Frontend Agent
- Backend Agent
- Database Agent
- Fix Agent

### Quality and release team

- Test Generation Agent
- Test Execution Agent
- Security Review Agent
- Code Review Agent
- Release Readiness Agent

## Runtime components

### Agent Registry

Stores agent metadata, versions, capabilities, permissions, prompts, configuration schemas, and health state.

### Orchestrator

Receives a goal, selects an approved workflow, resolves suitable agents, and coordinates execution.

### Planner

Transforms a goal into structured tasks, dependencies, risks, acceptance evidence, and approval requirements.

### Workflow Engine

Persists workflow state, supports pause/resume, waits for approvals, retries safe operations, and compensates for failed actions.

### Tool Gateway

Provides normalized access to repository, filesystem, terminal, test, browser, database, and deployment tools. The gateway validates permissions before execution.

### Policy Engine

Evaluates organization policy, user role, repository sensitivity, action risk, environment, budget, and data classification.

### Memory and Context Service

Retrieves relevant files, architectural decisions, coding standards, recent changes, issue context, and prior validated outcomes.

### Evaluation Service

Checks structured output, compilation, tests, security policies, acceptance criteria, hallucination indicators, and task-specific quality metrics.

## Execution states

```text
CREATED
QUEUED
PLANNING
WAITING_FOR_APPROVAL
RUNNING
WAITING_FOR_TOOL
VALIDATING
REVIEWING
COMPLETED
FAILED
CANCELLED
ESCALATED
```

## Risk levels

- **Low:** Read-only search, explanation, documentation draft
- **Medium:** File changes on isolated branch, dependency update, test execution
- **High:** Database migration, secret access, infrastructure changes, deployment
- **Critical:** Production data mutation, protected-branch modification, destructive operation

High and critical actions always require explicit approval in the initial releases.

## Agent communication

Agents communicate through structured artifacts and events rather than unrestricted free-form conversations. Every handoff includes:

- Goal
- Inputs and source references
- Assumptions
- Proposed action
- Expected output schema
- Validation criteria
- Risk level
- Remaining budget

## Feedback-to-fix workflow

```text
FeedbackReceived
→ Intake Agent normalizes content
→ Classification Agent selects category
→ Duplicate Agent links similar reports
→ Priority Agent scores impact and urgency
→ Routing Agent selects owner workflow
→ Reproduction Agent verifies the issue
→ Planner Agent proposes implementation
→ Human approval based on risk
→ Fix Agent changes code on a branch
→ Test Agents validate
→ Review Agents inspect quality and security
→ Pull request is created
→ Release Readiness Agent recommends rollout
```

## Safety defaults

- Read-only by default
- No direct production access
- No secret values in prompts or logs
- Repository modifications only on agent-created branches
- Command allowlist and resource quotas in sandbox
- Network egress restricted by policy
- Complete audit trail
- Cancellation and emergency stop available to authorized users
