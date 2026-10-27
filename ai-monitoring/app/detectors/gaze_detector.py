"""
Gaze and Eye-Region Detector using Facial Blendshapes and Landmarks.
Estimates approximate gaze direction and distinguishes head movement from eye movement.
"""
import math
import numpy as np
from typing import Optional, List, Any, Dict
from app.schemas.monitoring import GazeData


class GazeDetector:
    def __init__(self, horizontal_threshold: float = 0.50, vertical_threshold: float = 0.55):
        self.horizontal_threshold = horizontal_threshold
        self.vertical_threshold = vertical_threshold

    def estimate_gaze(
        self,
        face_landmarks: Any,
        blendshapes: Optional[List[Any]] = None
    ) -> Optional[GazeData]:
        """
        Estimates gaze direction relative to the face using blendshapes and iris landmarks.
        Returns GazeData with horizontalScore (-1.0 to 1.0) and verticalScore (-1.0 to 1.0).
        """
        horiz_score = 0.0
        vert_score = 0.0

        # Method 1: MediaPipe Face Blendshapes (Primary & most reliable for eye movements)
        if blendshapes and len(blendshapes) > 0:
            categories = blendshapes[0] if isinstance(blendshapes[0], list) else blendshapes
            bdict: Dict[str, float] = {}
            for item in categories:
                if hasattr(item, "category_name") and hasattr(item, "score"):
                    bdict[item.category_name] = float(item.score)

            look_left = (bdict.get("eyeLookOutLeft", 0.0) + bdict.get("eyeLookInRight", 0.0)) / 2.0
            look_right = (bdict.get("eyeLookInLeft", 0.0) + bdict.get("eyeLookOutRight", 0.0)) / 2.0
            look_up = (bdict.get("eyeLookUpLeft", 0.0) + bdict.get("eyeLookUpRight", 0.0)) / 2.0
            look_down = (bdict.get("eyeLookDownLeft", 0.0) + bdict.get("eyeLookDownRight", 0.0)) / 2.0

            horiz_score = look_right - look_left  # negative = left, positive = right
            vert_score = look_up - look_down      # positive = up, negative = down

        # Method 2: Landmark geometric fallback if blendshapes are unavailable
        elif face_landmarks and len(face_landmarks) > 473:
            try:
                # Left eye: iris center (468), inner (133), outer (33)
                # Right eye: iris center (473), inner (362), outer (263)
                l_iris = face_landmarks[468]
                l_inner = face_landmarks[133]
                l_outer = face_landmarks[33]

                r_iris = face_landmarks[473]
                r_inner = face_landmarks[362]
                r_outer = face_landmarks[263]

                def eye_ratio(iris, inner, outer):
                    d_inner = math.hypot(iris.x - inner.x, iris.y - inner.y)
                    d_outer = math.hypot(iris.x - outer.x, iris.y - outer.y)
                    total = d_inner + d_outer
                    return (d_outer / total) if total > 1e-6 else 0.5

                l_ratio = eye_ratio(l_iris, l_inner, l_outer)
                r_ratio = eye_ratio(r_iris, r_inner, r_outer)
                avg_ratio = (l_ratio + r_ratio) / 2.0

                # Map 0.5 to 0.0, range [0.0, 1.0] to [-1.0, 1.0]
                horiz_score = (avg_ratio - 0.5) * 2.0
            except Exception:
                pass

        direction = "CENTER"
        if horiz_score > self.horizontal_threshold:
            direction = "RIGHT"
        elif horiz_score < -self.horizontal_threshold:
            direction = "LEFT"
        elif vert_score > self.vertical_threshold:
            direction = "UP"
        elif vert_score < -self.vertical_threshold:
            direction = "DOWN"

        return GazeData(
            horizontalScore=round(float(horiz_score), 2),
            verticalScore=round(float(vert_score), 2),
            direction=direction
        )
