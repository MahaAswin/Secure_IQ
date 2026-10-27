"""
Tests for RulesEngine: duration thresholds, false-positive reduction, and cooldowns.
"""
import pytest
from app.detectors.rules_engine import RulesEngine
from app.schemas.monitoring import HeadPoseData, GazeData


def test_no_face_duration_and_cooldown():
    engine = RulesEngine(
        face_absence_threshold=3.0,
        multiple_face_threshold=2.0,
        look_away_threshold=3.0,
        gaze_away_threshold=3.5,
        cooldown_seconds=10.0
    )
    attempt_id = 101

    # Time 0.0: No face detected initially -> should not trigger alert immediately
    alerts_0 = engine.evaluate_frame(attempt_id, faces_count=0, head_pose=None, gaze=None, current_time=1000.0)
    assert len(alerts_0) == 0

    # Time 1.5s: Still no face (below 3.0s threshold) -> no alert
    alerts_1 = engine.evaluate_frame(attempt_id, faces_count=0, head_pose=None, gaze=None, current_time=1001.5)
    assert len(alerts_1) == 0

    # Time 3.5s: Sustained absence exceeds 3.0s -> qualifies for NO_FACE alert
    alerts_2 = engine.evaluate_frame(attempt_id, faces_count=0, head_pose=None, gaze=None, current_time=1003.5)
    assert len(alerts_2) == 1
    assert alerts_2[0].violationType == "NO_FACE"
    assert alerts_2[0].severity == "HIGH"
    assert "No face detected" in alerts_2[0].description

    # Time 5.0s: Continuing absence during cooldown period (cooldown is 10s) -> no duplicate alert
    alerts_3 = engine.evaluate_frame(attempt_id, faces_count=0, head_pose=None, gaze=None, current_time=1005.0)
    assert len(alerts_3) == 0

    # Time 14.0s: Past cooldown (1003.5 + 10 = 1013.5) -> can trigger subsequent alert
    alerts_4 = engine.evaluate_frame(attempt_id, faces_count=0, head_pose=None, gaze=None, current_time=1014.0)
    assert len(alerts_4) == 1
    assert alerts_4[0].violationType == "NO_FACE"


def test_multiple_face_detection():
    engine = RulesEngine(
        face_absence_threshold=3.0,
        multiple_face_threshold=2.0,
        look_away_threshold=3.0,
        cooldown_seconds=10.0
    )
    attempt_id = 102

    # Initial frame with 2 faces
    alerts_0 = engine.evaluate_frame(attempt_id, faces_count=2, head_pose=None, gaze=None, current_time=1000.0)
    assert len(alerts_0) == 0

    # Frame at 2.5s -> qualifies for MULTIPLE_PERSON
    alerts_1 = engine.evaluate_frame(attempt_id, faces_count=2, head_pose=None, gaze=None, current_time=1002.5)
    assert len(alerts_1) == 1
    assert alerts_1[0].violationType == "MULTIPLE_PERSON"
    assert alerts_1[0].severity == "CRITICAL"


def test_head_pose_look_away():
    engine = RulesEngine(look_away_threshold=3.0, cooldown_seconds=10.0)
    attempt_id = 103

    head_left = HeadPoseData(yaw=-35.0, pitch=0.0, roll=0.0, direction="LEFT")
    head_center = HeadPoseData(yaw=0.0, pitch=0.0, roll=0.0, direction="CENTER")

    # Brief glance (1.0s) then return to center -> false-positive reduction
    engine.evaluate_frame(attempt_id, faces_count=1, head_pose=head_left, gaze=None, current_time=1000.0)
    engine.evaluate_frame(attempt_id, faces_count=1, head_pose=head_left, gaze=None, current_time=1001.0)
    # Returns to center before reaching 3.0s threshold
    alerts_back = engine.evaluate_frame(attempt_id, faces_count=1, head_pose=head_center, gaze=None, current_time=1001.5)
    assert len(alerts_back) == 0

    # Sustained look away for 3.5s
    engine.evaluate_frame(attempt_id, faces_count=1, head_pose=head_left, gaze=None, current_time=1010.0)
    alerts_sustained = engine.evaluate_frame(attempt_id, faces_count=1, head_pose=head_left, gaze=None, current_time=1013.5)
    assert len(alerts_sustained) == 1
    assert alerts_sustained[0].violationType == "HEAD_POSE"
    assert "LEFT" in alerts_sustained[0].description


def test_eye_movement_detection():
    engine = RulesEngine(gaze_away_threshold=3.0, cooldown_seconds=10.0)
    attempt_id = 104

    head_center = HeadPoseData(yaw=0.0, pitch=0.0, roll=0.0, direction="CENTER")
    gaze_right = GazeData(horizontalScore=0.75, verticalScore=0.0, direction="RIGHT")

    # Head is center but gaze is sustained to the right
    engine.evaluate_frame(attempt_id, faces_count=1, head_pose=head_center, gaze=gaze_right, current_time=1000.0)
    alerts = engine.evaluate_frame(attempt_id, faces_count=1, head_pose=head_center, gaze=gaze_right, current_time=1003.5)
    assert len(alerts) == 1
    assert alerts[0].violationType == "EYE_MOVEMENT"
    assert alerts[0].severity == "LOW"
