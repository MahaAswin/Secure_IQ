"""
Face Detector using MediaPipe FaceLandmarker.
Detects face count, presence, and extracts landmarks and blendshapes.
"""
import logging
import cv2
import numpy as np
import mediapipe as mp
from typing import List, Optional, Tuple, Any

logger = logging.getLogger(__name__)


class FaceDetectionResult:
    def __init__(
        self,
        faces_count: int,
        landmarks_list: List[Any],
        blendshapes_list: List[Any],
        confidence: float
    ):
        self.faces_count = faces_count
        self.landmarks_list = landmarks_list  # List of NormalizedLandmark lists
        self.blendshapes_list = blendshapes_list
        self.confidence = confidence


class FaceDetector:
    def __init__(self, model_path: str, min_confidence: float = 0.5):
        self.model_path = model_path
        self.min_confidence = min_confidence
        self.landmarker = None
        self._initialize_detector()

    def _initialize_detector(self):
        try:
            BaseOptions = mp.tasks.BaseOptions
            FaceLandmarker = mp.tasks.vision.FaceLandmarker
            FaceLandmarkerOptions = mp.tasks.vision.FaceLandmarkerOptions
            VisionRunningMode = mp.tasks.vision.RunningMode

            options = FaceLandmarkerOptions(
                base_options=BaseOptions(model_asset_path=self.model_path),
                running_mode=VisionRunningMode.IMAGE,
                num_faces=4,  # Detect up to 4 faces to catch multiple people in camera
                min_face_detection_confidence=self.min_confidence,
                min_face_presence_confidence=self.min_confidence,
                min_tracking_confidence=self.min_confidence,
                output_face_blendshapes=True
            )
            self.landmarker = FaceLandmarker.create_from_options(options)
            logger.info("FaceLandmarker initialized successfully from %s", self.model_path)
        except Exception as e:
            logger.error("Failed to initialize FaceLandmarker: %s", e)
            self.landmarker = None

    def detect(self, bgr_image: np.ndarray) -> FaceDetectionResult:
        """
        Processes a BGR OpenCV image and returns detected faces, landmarks, and blendshapes.
        """
        if bgr_image is None or bgr_image.size == 0:
            return FaceDetectionResult(0, [], [], 0.0)

        if self.landmarker is None:
            logger.warning("FaceLandmarker not initialized. Attempting re-initialization.")
            self._initialize_detector()
            if self.landmarker is None:
                return FaceDetectionResult(0, [], [], 0.0)

        try:
            # Convert BGR to RGB
            rgb_image = cv2.cvtColor(bgr_image, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb_image)

            detection_result = self.landmarker.detect(mp_image)

            faces_count = len(detection_result.face_landmarks) if detection_result.face_landmarks else 0
            landmarks = detection_result.face_landmarks if detection_result.face_landmarks else []
            blendshapes = detection_result.face_blendshapes if detection_result.face_blendshapes else []

            # Confidence estimate
            confidence = 0.95 if faces_count == 1 else (0.85 if faces_count > 1 else 0.0)

            return FaceDetectionResult(
                faces_count=faces_count,
                landmarks_list=landmarks,
                blendshapes_list=blendshapes,
                confidence=confidence
            )
        except Exception as e:
            logger.error("Error during face detection: %s", e)
            return FaceDetectionResult(0, [], [], 0.0)
