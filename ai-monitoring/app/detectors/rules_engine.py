"""
Rules Engine for False-Positive Reduction, Duration Thresholds, and Cooldown Logic.
Translates raw frame detections into qualified SecureIQ violation events.
"""
import time
from datetime import datetime, timezone
from typing import Dict, List, Optional
from app.schemas.monitoring import HeadPoseData, GazeData, ViolationAlert
from app.config import settings


class AttemptTrackingState:
    def __init__(self, attempt_id: int):
        self.attempt_id = attempt_id
        self.created_at = time.time()
        self.frames_processed = 0
        self.violations_logged = 0

        # State timestamps (when an anomalous condition started)
        self.no_face_start: Optional[float] = None
        self.multiple_face_start: Optional[float] = None
        self.head_pose_start: Optional[float] = None
        self.gaze_start: Optional[float] = None

        # Cooldown tracking (when the last alert for each type was generated)
        self.last_alert_time: Dict[str, float] = {
            "NO_FACE": 0.0,
            "MULTIPLE_PERSON": 0.0,
            "HEAD_POSE": 0.0,
            "EYE_MOVEMENT": 0.0
        }


class RulesEngine:
    def __init__(
        self,
        face_absence_threshold: float = settings.FACE_ABSENCE_THRESHOLD_SECONDS,
        multiple_face_threshold: float = settings.MULTIPLE_FACE_THRESHOLD_SECONDS,
        look_away_threshold: float = settings.LOOK_AWAY_THRESHOLD_SECONDS,
        gaze_away_threshold: float = settings.GAZE_AWAY_THRESHOLD_SECONDS,
        cooldown_seconds: float = settings.DETECTION_COOLDOWN_SECONDS
    ):
        self.face_absence_threshold = face_absence_threshold
        self.multiple_face_threshold = multiple_face_threshold
        self.look_away_threshold = look_away_threshold
        self.gaze_away_threshold = gaze_away_threshold
        self.cooldown_seconds = cooldown_seconds

        # Map of attempt_id -> AttemptTrackingState
        self.states: Dict[int, AttemptTrackingState] = {}

    def get_or_create_state(self, attempt_id: int) -> AttemptTrackingState:
        if attempt_id not in self.states:
            self.states[attempt_id] = AttemptTrackingState(attempt_id)
        return self.states[attempt_id]

    def remove_state(self, attempt_id: int) -> Optional[AttemptTrackingState]:
        return self.states.pop(attempt_id, None)

    def evaluate_frame(
        self,
        attempt_id: int,
        faces_count: int,
        head_pose: Optional[HeadPoseData],
        gaze: Optional[GazeData],
        current_time: Optional[float] = None
    ) -> List[ViolationAlert]:
        """
        Evaluates frame data against rules, smoothing filters, duration thresholds, and cooldowns.
        Returns a list of qualified ViolationAlert objects triggered in this frame.
        """
        if current_time is None:
            current_time = time.time()

        state = self.get_or_create_state(attempt_id)
        state.frames_processed += 1
        alerts: List[ViolationAlert] = []

        now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")

        # 1. NO_FACE Rule
        if faces_count == 0:
            if state.no_face_start is None:
                state.no_face_start = current_time

            duration = current_time - state.no_face_start
            time_since_last = current_time - state.last_alert_time["NO_FACE"]

            if duration >= self.face_absence_threshold and time_since_last >= self.cooldown_seconds:
                alert = ViolationAlert(
                    violationCode=f"VIO-{attempt_id}-NF-{int(current_time * 1000)}",
                    studentExamAttemptId=attempt_id,
                    violationType="NO_FACE",
                    severity="HIGH",
                    source="AI_ENGINE",
                    description=f"No face detected in camera feed for {duration:.1f} seconds",
                    confidenceScore=95.0,
                    detectedAt=now_iso,
                    actionTaken="LOGGED_BY_AI"
                )
                alerts.append(alert)
                state.last_alert_time["NO_FACE"] = current_time
                state.violations_logged += 1
        else:
            state.no_face_start = None

        # 2. MULTIPLE_PERSON Rule
        if faces_count > 1:
            if state.multiple_face_start is None:
                state.multiple_face_start = current_time

            duration = current_time - state.multiple_face_start
            time_since_last = current_time - state.last_alert_time["MULTIPLE_PERSON"]

            if duration >= self.multiple_face_threshold and time_since_last >= self.cooldown_seconds:
                alert = ViolationAlert(
                    violationCode=f"VIO-{attempt_id}-MP-{int(current_time * 1000)}",
                    studentExamAttemptId=attempt_id,
                    violationType="MULTIPLE_PERSON",
                    severity="CRITICAL",
                    source="AI_ENGINE",
                    description=f"Multiple faces ({faces_count}) detected in camera feed for {duration:.1f} seconds",
                    confidenceScore=90.0,
                    detectedAt=now_iso,
                    actionTaken="LOGGED_BY_AI"
                )
                alerts.append(alert)
                state.last_alert_time["MULTIPLE_PERSON"] = current_time
                state.violations_logged += 1
        else:
            state.multiple_face_start = None

        # 3. HEAD_POSE Rule (Only applicable if exactly 1 face is present)
        if faces_count == 1 and head_pose is not None:
            if head_pose.direction != "CENTER":
                if state.head_pose_start is None:
                    state.head_pose_start = current_time

                duration = current_time - state.head_pose_start
                time_since_last = current_time - state.last_alert_time["HEAD_POSE"]

                if duration >= self.look_away_threshold and time_since_last >= self.cooldown_seconds:
                    alert = ViolationAlert(
                        violationCode=f"VIO-{attempt_id}-HP-{int(current_time * 1000)}",
                        studentExamAttemptId=attempt_id,
                        violationType="HEAD_POSE",
                        severity="MEDIUM",
                        source="AI_ENGINE",
                        description=(
                            f"Sustained head orientation away from screen "
                            f"(Head turned {head_pose.direction}: yaw={head_pose.yaw}°, pitch={head_pose.pitch}°) "
                            f"for {duration:.1f} seconds"
                        ),
                        confidenceScore=85.0,
                        detectedAt=now_iso,
                        actionTaken="LOGGED_BY_AI"
                    )
                    alerts.append(alert)
                    state.last_alert_time["HEAD_POSE"] = current_time
                    state.violations_logged += 1
            else:
                state.head_pose_start = None
        else:
            state.head_pose_start = None

        # 4. EYE_MOVEMENT Rule (Only if head is centered so we distinguish eye gaze from head turn)
        if faces_count == 1 and head_pose is not None and head_pose.direction == "CENTER" and gaze is not None:
            if gaze.direction != "CENTER":
                if state.gaze_start is None:
                    state.gaze_start = current_time

                duration = current_time - state.gaze_start
                time_since_last = current_time - state.last_alert_time["EYE_MOVEMENT"]

                if duration >= self.gaze_away_threshold and time_since_last >= self.cooldown_seconds:
                    alert = ViolationAlert(
                        violationCode=f"VIO-{attempt_id}-EM-{int(current_time * 1000)}",
                        studentExamAttemptId=attempt_id,
                        violationType="EYE_MOVEMENT",
                        severity="LOW",
                        source="AI_ENGINE",
                        description=(
                            f"Sustained eye-region gaze away from screen "
                            f"(Gaze direction {gaze.direction}, horiz={gaze.horizontalScore}) "
                            f"for {duration:.1f} seconds"
                        ),
                        confidenceScore=75.0,
                        detectedAt=now_iso,
                        actionTaken="LOGGED_BY_AI"
                    )
                    alerts.append(alert)
                    state.last_alert_time["EYE_MOVEMENT"] = current_time
                    state.violations_logged += 1
            else:
                state.gaze_start = None
        else:
            state.gaze_start = None

        return alerts
