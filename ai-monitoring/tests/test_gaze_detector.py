"""
Tests for GazeDetector using blendshapes.
"""
import pytest
from app.detectors.gaze_detector import GazeDetector


class MockCategory:
    def __init__(self, name: str, score: float):
        self.category_name = name
        self.score = score


def test_gaze_center():
    detector = GazeDetector(horizontal_threshold=0.5, vertical_threshold=0.5)
    blendshapes = [
        MockCategory("eyeLookInLeft", 0.05),
        MockCategory("eyeLookOutLeft", 0.05),
        MockCategory("eyeLookInRight", 0.05),
        MockCategory("eyeLookOutRight", 0.05),
        MockCategory("eyeLookUpLeft", 0.05),
        MockCategory("eyeLookDownLeft", 0.05),
    ]
    gaze = detector.estimate_gaze(None, blendshapes=[blendshapes])
    assert gaze is not None
    assert gaze.direction == "CENTER"
    assert abs(gaze.horizontalScore) < 0.2


def test_gaze_left():
    detector = GazeDetector(horizontal_threshold=0.5)
    # Looking left: eyeLookOutLeft is high, eyeLookInRight is high
    blendshapes = [
        MockCategory("eyeLookOutLeft", 0.8),
        MockCategory("eyeLookInRight", 0.75),
        MockCategory("eyeLookInLeft", 0.05),
        MockCategory("eyeLookOutRight", 0.05),
    ]
    gaze = detector.estimate_gaze(None, blendshapes=[blendshapes])
    assert gaze is not None
    assert gaze.direction == "LEFT"
    assert gaze.horizontalScore < -0.5


def test_gaze_right():
    detector = GazeDetector(horizontal_threshold=0.5)
    # Looking right: eyeLookInLeft is high, eyeLookOutRight is high
    blendshapes = [
        MockCategory("eyeLookInLeft", 0.8),
        MockCategory("eyeLookOutRight", 0.75),
        MockCategory("eyeLookOutLeft", 0.05),
        MockCategory("eyeLookInRight", 0.05),
    ]
    gaze = detector.estimate_gaze(None, blendshapes=[blendshapes])
    assert gaze is not None
    assert gaze.direction == "RIGHT"
    assert gaze.horizontalScore > 0.5
