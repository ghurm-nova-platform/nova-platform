# Nova Agent Runtime

Provider-neutral Python service that registers agents, plans and coordinates workflows, invokes approved tools, tracks execution state, and emits auditable events.

## Baseline

- Python 3.12
- FastAPI and Pydantic
- PostgreSQL for durable state
- Redis for short-lived coordination
- RabbitMQ for asynchronous events

## Safety

Agents are read-only by default. File writes, terminal commands, Git pushes, deployments, and database changes require policy evaluation and may require human approval.
