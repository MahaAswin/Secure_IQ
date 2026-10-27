"""
Head Pose Detector using 3D Facial Landmarks and Perspective-n-Point (solvePnP).
Estimates Yaw, Pitch, Roll angles and determines head orientation direction.
"""
import cv2
import math
import numpy as np
from typing import Tuple, Optional, Any
from app.schemas.monitoring import HeadPoseData


class HeadPoseDetector:
    def __init__(
        self,
        yaw_threshold: float = 25.0,
        pitch_up_threshold: float = 20.0,
        pitch_down_threshold: float = 25.0
    ):
        self.yaw_threshold = yaw_threshold
        self.pitch_up_threshold = pitch_up_threshold
        self.pitch_down_threshold = pitch_down_threshold

        # Canonical 3D facial model points (in arbitrary world coordinates)
        self.model_points = np.array([
            (0.0, 0.0, 0.0),             # Nose tip (landmark 1)
            (0.0, -330.0, -65.0),        # Chin (landmark 152)
            (-225.0, 170.0, -135.0),     # Left eye corner (landmark 33)
            (225.0, 170.0, -135.0),      # Right eye corner (landmark 263)
            (-150.0, -150.0, -125.0),    # Left mouth corner (landmark 61)
            (150.0, -150.0, -125.0)      # Right mouth corner (landmark 291)
        ], dtype=np.float64)

    def estimate_pose(
        self,
        face_landmarks: Any,
        image_width: int,
        image_height: int
    ) -> Optional[HeadPoseData]:
        """
        Calculates head orientation from facial landmarks.
        Returns HeadPoseData with yaw, pitch, roll in degrees and qualitative direction.
        """
        if not face_landmarks:
            return None

        try:
            # Extract relevant 2D pixel coordinates
            # NormalizedLandmarks have .x and .y in [0.0, 1.0]
            landmarks = face_landmarks
            image_points = np.array([
                (landmarks[1].x * image_width, landmarks[1].y * image_height),      # Nose tip
                (landmarks[152].x * image_width, landmarks[152].y * image_height),  # Chin
                (landmarks[33].x * image_width, landmarks[33].y * image_height),    # Left eye corner
                (landmarks[263].x * image_width, landmarks[263].y * image_height),  # Right eye corner
                (landmarks[61].x * image_width, landmarks[61].y * image_height),    # Left mouth corner
                (landmarks[291].x * image_width, landmarks[291].y * image_height)   # Right mouth corner
            ], dtype=np.float64)

            # Approximation of camera matrix
            focal_length = image_width
            center = (image_width / 2.0, image_height / 2.0)
            camera_matrix = np.array([
                [focal_length, 0, center[0]],
                [0, focal_length, center[1]],
                [0, 0, 1]
            ], dtype=np.float64)
            dist_coeffs = np.zeros((4, 1))

            success, rvec, tvec = cv2.solvePnP(
                self.model_points,
                image_points,
                camera_matrix,
                dist_coeffs,
                flags=cv2.SOLVEPNP_ITERATIVE
            )

            if not success:
                return None

            rmat, _ = cv2.Rodrigues(rvec)

            # Decompose rotation matrix into Euler angles
            sy = math.sqrt(rmat[0, 0] * rmat[0, 0] + rmat[1, 0] * rmat[1, 0])
            singular = sy < 1e-6

            if not singular:
                x = math.atan2(rmat[2, 1], rmat[2, 2])
                y = math.atan2(-rmat[2, 0], sy)
                z = math.atan2(rmat[1, 0], rmat[0, 0])
            else:
                x = math.atan2(-rmat[1, 2], rmat[1, 1])
                y = math.atan2(-rmat[2, 0], sy)
                z = 0

            raw_pitch = math.degrees(x)
            raw_yaw = math.degrees(y)
            roll = math.degrees(z)

            # Normalize yaw: negative = turning left, positive = turning right
            yaw = -raw_yaw

            # Normalize pitch around canonical straight gaze
            if raw_pitch > 0:
                pitch = 160.0 - raw_pitch
            else:
                pitch = -160.0 - raw_pitch

            # Determine dominant direction
            direction = "CENTER"
            if yaw > self.yaw_threshold:
                direction = "RIGHT"
            elif yaw < -self.yaw_threshold:
                direction = "LEFT"
            elif pitch > self.pitch_up_threshold:
                direction = "UP"
            elif pitch < -self.pitch_down_threshold:
                direction = "DOWN"

            return HeadPoseData(
                yaw=round(float(yaw), 1),
                pitch=round(float(pitch), 1),
                roll=round(float(roll), 1),
                direction=direction
            )
        except Exception:
            return None
