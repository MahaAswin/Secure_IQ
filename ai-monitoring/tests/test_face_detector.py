"""
Tests for FaceDetector with synthetic images.
"""
import pytest
import numpy as np
from app.detectors.face_detector import FaceDetector
from app.config import settings


def test_face_detector_empty_frame():
    detector = FaceDetector(settings.MODEL_PATH)
    # Empty image
    empty_img = np.zeros((0, 0, 3), dtype=np.uint8)
    res = detector.detect(empty_img)
    assert res.faces_count == 0
    assert len(res.landmarks_list) == 0


def test_face_detector_no_face_black_image():
    detector = FaceDetector(settings.MODEL_PATH)
    # Black 640x480 frame
    black_img = np.zeros((480, 640, 3), dtype=np.uint8)
    res = detector.detect(black_img)
    assert res.faces_count == 0
    assert len(res.landmarks_list) == 0
    assert res.confidence == 0.0
