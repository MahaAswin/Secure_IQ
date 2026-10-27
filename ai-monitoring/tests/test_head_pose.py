"""
Tests for HeadPoseDetector using synthetic facial landmarks.
"""
import pytest
from app.detectors.head_pose_detector import HeadPoseDetector


class MockLandmark:
    def __init__(self, x: float, y: float, z: float = 0.0):
        self.x = x
        self.y = y
        self.z = z


def create_mock_landmarks(dx: float = 0.0, dy: float = 0.0):
    """
    Creates normalized 478 mock landmarks with adjustable nose offset.
    """
    # 640x480 normalized coordinates
    # Center points:
    # Nose (1): (0.5 + dx, 0.5 + dy)
    # Chin (152): (0.5, 0.75)
    # Left eye corner (33): (0.375, 0.416)
    # Right eye corner (263): (0.625, 0.416)
    # Left mouth (61): (0.421, 0.645)
    # Right mouth (291): (0.578, 0.645)
    landmarks = [MockLandmark(0.5, 0.5) for _ in range(478)]
    landmarks[1] = MockLandmark(0.5 + dx, 0.5 + dy)
    landmarks[152] = MockLandmark(0.5, 0.75)
    landmarks[33] = MockLandmark(0.375, 0.416)
    landmarks[263] = MockLandmark(0.625, 0.416)
    landmarks[61] = MockLandmark(0.421, 0.645)
    landmarks[291] = MockLandmark(0.578, 0.645)
    return landmarks


def test_head_pose_center():
    detector = HeadPoseDetector(yaw_threshold=20.0, pitch_up_threshold=20.0, pitch_down_threshold=25.0)
    landmarks = create_mock_landmarks(dx=0.0, dy=0.0)
    pose = detector.estimate_pose(landmarks, 640, 480)

    assert pose is not None
    assert pose.direction == "CENTER"
    assert abs(pose.yaw) < 15.0


def test_head_pose_turned_right():
    detector = HeadPoseDetector(yaw_threshold=20.0)
    # Nose shifted right in image coordinates -> yaw positive (turned right)
    landmarks = create_mock_landmarks(dx=0.08, dy=0.0)
    pose = detector.estimate_pose(landmarks, 640, 480)

    assert pose is not None
    assert pose.direction == "RIGHT"
    assert pose.yaw > 20.0


def test_head_pose_turned_left():
    detector = HeadPoseDetector(yaw_threshold=20.0)
    # Nose shifted left in image coordinates -> yaw negative (turned left)
    landmarks = create_mock_landmarks(dx=-0.08, dy=0.0)
    pose = detector.estimate_pose(landmarks, 640, 480)

    assert pose is not None
    assert pose.direction == "LEFT"
    assert pose.yaw < -20.0
