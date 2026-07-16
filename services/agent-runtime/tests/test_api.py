"""API endpoint tests."""

from __future__ import annotations

from fastapi.testclient import TestClient

TEST_ACTOR = "agent-runtime-test-actor"


def test_health(client: TestClient) -> None:
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"
    assert response.headers.get("X-Correlation-Id")


def test_missing_api_key_returns_401(client: TestClient) -> None:
    response = client.get("/agents")
    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_invalid_api_key_returns_401(client: TestClient) -> None:
    response = client.get("/agents", headers={"X-API-Key": "wrong-key"})
    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_register_and_list_agents(client: TestClient, auth_headers: dict[str, str]) -> None:
    payload = {
        "agent_id": "code.search",
        "version": "1.0.0",
        "display_name": "Code Search Agent",
        "description": "Read-only repository search",
        "capabilities": ["code_search"],
    }
    created = client.post("/agents/register", json=payload, headers=auth_headers)
    assert created.status_code == 201
    assert created.json()["agent_id"] == "code.search"

    listed = client.get("/agents", headers=auth_headers)
    assert listed.status_code == 200
    assert len(listed.json()) == 1


def test_duplicate_agent_registration_conflicts(
    client: TestClient, auth_headers: dict[str, str]
) -> None:
    payload = {
        "agent_id": "planner.agent",
        "version": "1.0.0",
        "display_name": "Planner",
        "description": "Plans work",
    }
    assert (
        client.post("/agents/register", json=payload, headers=auth_headers).status_code
        == 201
    )
    conflict = client.post("/agents/register", json=payload, headers=auth_headers)
    assert conflict.status_code == 409
    assert conflict.json()["code"] == "CONFLICT"


def test_execute_agent_completes_for_low_risk(
    client: TestClient, auth_headers: dict[str, str]
) -> None:
    client.post(
        "/agents/register",
        headers=auth_headers,
        json={
            "agent_id": "docs.read",
            "version": "1.0.0",
            "display_name": "Docs Reader",
            "description": "Reads documentation",
            "required_permissions": ["READ", "EXECUTE"],
            "read_only_default": True,
        },
    )
    response = client.post(
        "/agents/execute",
        headers=auth_headers,
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
    assert body["output"]["actor"] == TEST_ACTOR


def test_actor_spoofing_is_impossible(
    client: TestClient, auth_headers: dict[str, str]
) -> None:
    client.post(
        "/agents/register",
        headers=auth_headers,
        json={
            "agent_id": "spoof.target",
            "version": "1.0.0",
            "display_name": "Spoof Target",
            "description": "Actor spoofing test agent",
            "required_permissions": ["READ", "EXECUTE"],
        },
    )
    response = client.post(
        "/agents/execute",
        headers=auth_headers,
        json={
            "agent_id": "spoof.target",
            "goal": "Attempt spoof",
            "actor": "totally-not-authenticated",
            "risk_level": "LOW",
        },
    )
    # Extra actor field is forbidden; spoofing via payload cannot succeed.
    assert response.status_code == 422

    accepted = client.post(
        "/agents/execute",
        headers=auth_headers,
        json={
            "agent_id": "spoof.target",
            "goal": "Trusted actor only",
            "risk_level": "LOW",
        },
    )
    assert accepted.status_code == 200
    assert accepted.json()["output"]["actor"] == TEST_ACTOR
    assert accepted.json()["output"]["actor"] != "totally-not-authenticated"


def test_readonly_agent_execution_still_requires_execute(
    client: TestClient, auth_headers: dict[str, str]
) -> None:
    client.post(
        "/agents/register",
        headers=auth_headers,
        json={
            "agent_id": "readonly.exec",
            "version": "1.0.0",
            "display_name": "Readonly",
            "description": "Read-only agent without EXECUTE",
            "required_permissions": ["READ"],
            "read_only_default": True,
        },
    )
    response = client.post(
        "/agents/execute",
        headers=auth_headers,
        json={
            "agent_id": "readonly.exec",
            "goal": "Should require EXECUTE",
            "risk_level": "LOW",
        },
    )
    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_high_risk_cannot_bypass_approval_using_dry_run(
    client: TestClient, auth_headers: dict[str, str]
) -> None:
    client.post(
        "/agents/register",
        headers=auth_headers,
        json={
            "agent_id": "fix.agent",
            "version": "1.0.0",
            "display_name": "Fix Agent",
            "description": "Proposes fixes",
            "required_permissions": ["READ", "EXECUTE"],
        },
    )
    # dry_run is forbidden on the request schema.
    rejected = client.post(
        "/agents/execute",
        headers=auth_headers,
        json={
            "agent_id": "fix.agent",
            "goal": "Apply a risky change",
            "risk_level": "HIGH",
            "dry_run": True,
        },
    )
    assert rejected.status_code == 422

    response = client.post(
        "/agents/execute",
        headers=auth_headers,
        json={
            "agent_id": "fix.agent",
            "goal": "Apply a risky change",
            "risk_level": "HIGH",
        },
    )
    assert response.status_code == 200
    assert response.json()["status"] == "WAITING_FOR_APPROVAL"


def test_client_cannot_lower_server_risk_baseline(
    client: TestClient, auth_headers: dict[str, str]
) -> None:
    client.post(
        "/agents/register",
        headers=auth_headers,
        json={
            "agent_id": "writer.agent",
            "version": "1.0.0",
            "display_name": "Writer",
            "description": "Write-capable agent",
            "required_permissions": ["READ", "EXECUTE", "WRITE"],
            "required_tools": ["repo.write_file"],
            "read_only_default": False,
        },
    )
    response = client.post(
        "/agents/execute",
        headers=auth_headers,
        json={
            "agent_id": "writer.agent",
            "goal": "Attempt to claim low risk",
            "risk_level": "LOW",
        },
    )
    assert response.status_code == 200
    assert response.json()["output"]["effective_risk_level"] == "HIGH"
    assert response.json()["status"] == "WAITING_FOR_APPROVAL"


def test_create_and_list_workflows(
    client: TestClient, auth_headers: dict[str, str]
) -> None:
    created = client.post(
        "/workflows",
        headers=auth_headers,
        json={"name": "triage", "goal": "Triage feedback item"},
    )
    assert created.status_code == 201
    assert created.json()["state"] == "QUEUED"

    listed = client.get("/workflows", headers=auth_headers)
    assert listed.status_code == 200
    assert len(listed.json()) == 1


def test_metrics_endpoint(client: TestClient, auth_headers: dict[str, str]) -> None:
    client.post(
        "/agents/register",
        headers=auth_headers,
        json={
            "agent_id": "metric.agent",
            "version": "1.0.0",
            "display_name": "Metric Agent",
            "description": "Used for metrics",
        },
    )
    response = client.get("/metrics", headers=auth_headers)
    assert response.status_code == 200
    body = response.json()
    assert body["agents_registered"] == 1
    assert "executions_total" in body


def test_validation_error_contract(
    client: TestClient, auth_headers: dict[str, str]
) -> None:
    response = client.post(
        "/agents/register",
        headers=auth_headers,
        json={"agent_id": "bad"},
    )
    assert response.status_code == 422
    body = response.json()
    assert body["code"] == "VALIDATION_FAILED"
    assert body["details"]
