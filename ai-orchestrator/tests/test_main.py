"""HTTP API tests for orchestrator authentication."""

from __future__ import annotations

import os

import pytest
from fastapi.testclient import TestClient

from app.config import get_settings
from app.main import app


@pytest.fixture()
def client(monkeypatch: pytest.MonkeyPatch) -> TestClient:
    monkeypatch.setenv("ROOMBAY_INTERNAL_TOKEN", "test-internal-token")
    monkeypatch.setenv("AI_PROVIDER", "stub")
    get_settings.cache_clear()
    yield TestClient(app)
    get_settings.cache_clear()


def test_health_does_not_require_token(client: TestClient) -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_orchestrate_rejects_missing_token(client: TestClient) -> None:
    response = client.post(
        "/orchestrate",
        json={
            "message": "find a room in Buea",
            "persona": "TENANT",
            "userId": "00000000-0000-0000-0000-000000000001",
            "userRole": "STUDENT",
        },
    )
    assert response.status_code == 401


def test_orchestrate_rejects_invalid_token(client: TestClient) -> None:
    response = client.post(
        "/orchestrate",
        headers={"X-RoomBay-Internal-Token": "wrong-token"},
        json={
            "message": "find a room in Buea",
            "persona": "TENANT",
            "userId": "00000000-0000-0000-0000-000000000001",
            "userRole": "STUDENT",
        },
    )
    assert response.status_code == 401


def test_orchestrate_accepts_valid_token(client: TestClient) -> None:
    response = client.post(
        "/orchestrate",
        headers={"X-RoomBay-Internal-Token": "test-internal-token"},
        json={
            "message": "find a room in Buea",
            "persona": "TENANT",
            "userId": "00000000-0000-0000-0000-000000000001",
            "userRole": "STUDENT",
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert "answer" in body
    assert body.get("meta") is not None


def test_orchestrate_closed_when_token_unconfigured(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delenv("ROOMBAY_INTERNAL_TOKEN", raising=False)
    get_settings.cache_clear()
    client = TestClient(app)
    try:
        response = client.post(
            "/orchestrate",
            headers={"X-RoomBay-Internal-Token": "anything"},
            json={"message": "hello", "persona": "TENANT", "userRole": "STUDENT"},
        )
        assert response.status_code == 401
    finally:
        get_settings.cache_clear()
        if "ROOMBAY_INTERNAL_TOKEN" in os.environ:
            pass
