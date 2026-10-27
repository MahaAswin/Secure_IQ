"""
Tests for FastAPI Monitoring Endpoints.
"""
import pytest
from fastapi.testclient import TestClient
from app.main import app
import base64
import numpy as np
import cv2

client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] in ("UP", "DEGRADED")
    assert "detector" in data


def test_monitoring_lifecycle():
    attempt_id = 999

    # 1. Start session
    start_res = client.post("/monitoring/start", json={
        "attemptId": attempt_id,
        "examId": 1,
        "token": "test-jwt-token"
    })
    assert start_res.status_code == 200
    start_data = start_res.json()
    assert start_data["status"] == "ACTIVE"
    assert start_data["attemptId"] == attempt_id

    # 2. Check status
    status_res = client.get(f"/monitoring/status/{attempt_id}")
    assert status_res.status_code == 200
    status_data = status_res.json()
    assert status_data["active"] is True

    # 3. Analyze dummy frame (solid black image: no face)
    dummy_img = np.zeros((240, 320, 3), dtype=np.uint8)
    _, buf = cv2.imencode(".jpg", dummy_img)
    b64_frame = base64.b64encode(buf).decode("utf-8")

    frame_res = client.post("/monitoring/frame", json={
        "attemptId": attempt_id,
        "frameBase64": b64_frame
    })
    assert frame_res.status_code == 200
    frame_data = frame_res.json()
    assert frame_data["facesDetected"] == 0
    assert frame_data["status"] in ("NORMAL", "SUSPICIOUS", "VIOLATION")

    # 4. Stop session
    stop_res = client.post("/monitoring/stop", json={"attemptId": attempt_id})
    assert stop_res.status_code == 200
    stop_data = stop_res.json()
    assert stop_data["status"] == "STOPPED"
    assert stop_data["totalFrames"] >= 1


def test_invalid_requests():
    # Attempt ID <= 0
    res_start = client.post("/monitoring/start", json={"attemptId": 0})
    assert res_start.status_code == 400

    # Empty frame
    res_frame = client.post("/monitoring/frame", json={"attemptId": 1, "frameBase64": ""})
    assert res_frame.status_code == 400
