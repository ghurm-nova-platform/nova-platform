# Product Roadmap

## Phase 0 — Foundation (Weeks 1–2)

- Approve project charter and architecture baseline
- Establish monorepo structure
- Add coding, security, and contribution standards
- Configure GitHub Actions baseline
- Define API and event contracts
- Create development environment with Docker Compose

**Exit criteria:** repository builds, tests, and validates documentation changes through CI.

## Phase 1 — Platform Skeleton (Weeks 3–6)

- Angular portal shell
- Spring Boot platform API
- PostgreSQL schema baseline
- Authentication foundation
- Organizations, workspaces, and projects
- Audit event storage
- Initial usage metering

**Exit criteria:** user can sign in, create an organization, workspace, and project.

## Phase 2 — Web IDE (Weeks 7–10)

- File explorer
- Monaco Editor
- Tabs and file persistence
- Project search
- Basic Git status and diff
- Terminal connection to isolated workspace
- Workflow timeline panel

**Exit criteria:** user can import a repository, edit files, run an approved command, and review changes.

## Phase 3 — Agent Runtime MVP (Weeks 11–14)

- Agent registry
- Model provider abstraction
- Tool gateway
- Workflow state machine
- Policy checks
- Read-only code search agent
- File-change agent restricted to branches
- Structured execution timeline

**Exit criteria:** an approved agent task can inspect a project and produce a validated branch diff.

## Phase 4 — Feedback Intelligence (Weeks 15–18)

- Feedback intake UI and API
- Classification agent
- Duplicate detection
- Priority scoring
- Routing workflows
- GitHub issue creation
- Draft fix workflow

**Exit criteria:** submitted feedback is classified, routed, linked to an issue, and can generate a reviewed draft patch.

## Phase 5 — Beta Hardening (Weeks 19–22)

- Security review
- Load and resilience testing
- Usage and cost controls
- Backup and recovery
- Product analytics
- Onboarding
- Ten-user closed beta

**Exit criteria:** Beta 0.1 is deployable with documented limitations, monitoring, rollback, and support procedures.

### Sprint 5 — Enterprise Dashboard (Phase 2)

- Read-only executive dashboard aggregating existing domain APIs
- Portal `/dashboard` with pipeline charts, tables, and 30s polling
- CSV/Excel/PDF export; in-memory cache with admin refresh

**Exit criteria:** Operators can view unified pipeline/release/deployment/audit signals without new operational tables or real-time infrastructure.

## After Beta

- Multiple collaborating engineering agents
- Knowledge and project memory
- Pull-request review automation
- Enterprise SSO and LDAP
- Self-hosted deployment
- Local model support
- Marketplace foundations
- Desktop client
