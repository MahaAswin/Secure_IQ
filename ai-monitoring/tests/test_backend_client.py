"""
Tests for BackendClient HTTP dispatching to Spring Boot API.
"""
import pytest
from unittest.mock import patch, AsyncMock, MagicMock
import httpx
from app.services.backend_client import BackendClient
from app.schemas.monitoring import ViolationAlert


@pytest.mark.anyio
async def test_backend_client_success():
    client = BackendClient(base_url="http://localhost:8080")
    client.register_token(101, "valid-jwt-token")

    alert = ViolationAlert(
        violationCode="VIO-TEST-001",
        studentExamAttemptId=101,
        violationType="NO_FACE",
        severity="HIGH",
        source="AI_CAMERA",
        description="No face detected for 3.5 seconds",
        confidenceScore=0.95,
        detectedAt="2026-10-27T00:00:00Z",
        actionTaken="LOGGED"
    )

    mock_response = MagicMock(status_code=201, text='{"id": 1}')
    with patch("httpx.AsyncClient.post", new_callable=AsyncMock) as mock_post:
        mock_post.return_value = mock_response
        success = await client.post_violation(alert)

        assert success is True
        mock_post.assert_called_once()
        args, kwargs = mock_post.call_args
        assert kwargs["headers"]["Authorization"] == "Bearer valid-jwt-token"
        assert kwargs["json"]["violationType"] == "NO_FACE"


@pytest.mark.anyio
async def test_backend_client_forbidden():
    client = BackendClient(base_url="http://localhost:8080")
    client.register_token(102, "unauthorized-token")

    alert = ViolationAlert(
        violationCode="VIO-TEST-002",
        studentExamAttemptId=102,
        violationType="MULTIPLE_PERSON",
        severity="CRITICAL",
        source="AI_CAMERA",
        description="Multiple persons detected",
        confidenceScore=0.85,
        detectedAt="2026-10-27T00:00:00Z",
        actionTaken="LOGGED"
    )

    mock_response = MagicMock(status_code=403, text='{"error": "Access denied"}')
    with patch("httpx.AsyncClient.post", new_callable=AsyncMock) as mock_post:
        mock_post.return_value = mock_response
        success = await client.post_violation(alert)

        assert success is False
        mock_post.assert_called_once()


@pytest.mark.anyio
async def test_backend_client_connection_error():
    client = BackendClient(base_url="http://localhost:8080")

    alert = ViolationAlert(
        violationCode="VIO-TEST-003",
        studentExamAttemptId=103,
        violationType="HEAD_POSE",
        severity="MEDIUM",
        source="AI_CAMERA",
        description="Looking away",
        confidenceScore=0.9,
        detectedAt="2026-10-27T00:00:00Z",
        actionTaken="LOGGED"
    )

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock) as mock_post:
        mock_post.side_effect = httpx.ConnectError("Connection refused")
        success = await client.post_violation(alert)

        assert success is False
