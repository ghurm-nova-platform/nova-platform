"""API endpoint tests."""

from __future__ import annotations

from fastapi.testclient import TestClient


def test_health(client: TestClient) -> None:
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"
    assert "correlation_id" not in body
    assert response.headers.get("X-Correlation-Id")


def test_register_and_list_agents(client: TestClient) -> None:
    payload = {
        "agent_id": "code.search",
        "version": "1.0.0",
        "display_name": "Code Search Agent",
        "description": "Read-only repository search",
        "capabilities": ["code_search"],
    }
    created = client.post("/agents/register", json=payload)
    assert created.status_code == 201
    assert created.json()["agent_id"] == "code.search"

    listed = client.get("/agents")
    assert listed.status_code == 200
    assert len(listed.json()) == 1


def test_duplicate_agent_registration_conflicts(client: TestClient) -> None:
    payload = {
        "agent_id": "planner.agent",
        "version": "1.0.0",
        "display_name": "Planner",
        "description": "Plans work",
    }
    assert client.post("/agents/register", json=payload).status_code == 201
    conflict = client.post("/agents/register", json=payload)
    assert conflict.status_code == 409
    assert conflict.json()["code"] == "CONFLICT"


def test_execute_agent_completes_for_low_risk(client: TestClient) -> None:
    client.post(
        "/agents/register",
        json={
            "agent_id": "docs.read",
            "version": "1.0.0",
            "display_name": "Docs Reader",
            "description": "Reads documentation",
        },
    )
    response = client.post(
        "/agents/execute",
        json={
            "agent_id": "docs.read",
            "goal": "Summarize architecture docs",
            "risk_level": "LOW",
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "COMPLETED"
    assert body["workflow_id"]
    assert body["output"]["provider"] == "none"


def test_execute_high_risk_waits_for_approval(client: TestClient) -> None:
    client.post(
        "/agents/register",
        json={
            "agent_id": "fix.agent",
            "version": "1.0.0",
            "display_name": "Fix Agent",
            "description": "Proposes fixes",
        },
    )
    response = client.post(
        "/agents/execute",
        json={
            "agent_id": "fix.agent",
            "goal": "Apply a risky change",
            "risk_level": "HIGH",
        },
    )
    assert response.status_code == 200
    assert response.json()["status"] == "WAITING_FOR_APPROVAL"


def test_create_and_list_workflows(client: TestClient) -> None:
    created = client.post(
        "/workflows",
        json={"name": "triage", "goal": "Triage feedback item"},
    )
    assert created.status_code == 201
    assert created.json()["state"] == "QUEUED"

    listed = client.get("/workflows")
    assert listed.status_code == 200
    assert len(listed.json()) == 1


def test_metrics_endpoint(client: TestClient) -> None:
    client.post(
        "/agents/register",
        json={
            "agent_id": "metric.agent",
            "version": "1.0.0",
            "display_name": "Metric Agent",
            "description": "Used for metrics",
        },
    )
    response = client.get("/metrics")
    assert response.status_code == 200
    body = response.json()
    assert body["agents_registered"] == 1
    assert "executions_total" in body


def test_validation_error_contract(client: TestClient) -> None:
    response = client.post("/agents/register", json={"agent_id": "bad"})
    assert response.status_code == 422
    body = response.json()
    assert body["code"] == "VALIDATION_FAILED"
    assert body["details"]
