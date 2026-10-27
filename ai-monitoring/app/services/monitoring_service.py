"""
Central Monitoring Service coordinating detectors, rules, and backend dispatch.
"""
import asyncio
import base64
import cv2
import logging
import numpy as np
import time
from typing import Dict, Optional, Tuple

from app.config import settings
from app.detectors import FaceDetector, HeadPoseDetector, GazeDetector, RulesEngine
from app.services.backend_client import BackendClient
from app.schemas.monitoring import (
    StartMonitoringRequest,
    StartMonitoringResponse,
    FrameAnalysisResponse,
    MonitoringStatusResponse,
    StopMonitoringResponse,
)

logger = logging.getLogger(__name__)


class MonitoringSession:
    def __init__(self, attempt_id: int, exam_id: Optional[int] = None, token: Optional[str] = None):
        self.attempt_id = attempt_id
        self.exam_id = exam_id
        self.token = token
        self.started_at = time.time()
        self.last_active_at = self.started_at
        self.active = True


class MonitoringService:
    def __init__(self):
        self.face_detector = FaceDetector(
            model_path=settings.MODEL_PATH,
            min_confidence=settings.MIN_FACE_DETECTION_CONFIDENCE
        )
        self.head_pose_detector = HeadPoseDetector(
            yaw_threshold=settings.HEAD_YAW_THRESHOLD,
            pitch_up_threshold=settings.HEAD_PITCH_UP_THRESHOLD,
            pitch_down_threshold=settings.HEAD_PITCH_DOWN_THRESHOLD
        )
        self.gaze_detector = GazeDetector(
            horizontal_threshold=settings.GAZE_HORIZONTAL_THRESHOLD
        )
        self.rules_engine = RulesEngine(
            face_absence_threshold=settings.FACE_ABSENCE_THRESHOLD_SECONDS,
            multiple_face_threshold=settings.MULTIPLE_FACE_THRESHOLD_SECONDS,
            look_away_threshold=settings.LOOK_AWAY_THRESHOLD_SECONDS,
            gaze_away_threshold=settings.GAZE_AWAY_THRESHOLD_SECONDS,
            cooldown_seconds=settings.DETECTION_COOLDOWN_SECONDS
        )
        self.backend_client = BackendClient(base_url=settings.SPRING_BOOT_API_BASE_URL)

        # Active monitoring sessions: attempt_id -> MonitoringSession
        self.sessions: Dict[int, MonitoringSession] = {}

    def cleanup_stale_sessions(self, max_inactivity_seconds: float = 60.0):
        """
        Cleans up stale or abandoned monitoring sessions that have not received frames.
        """
        now = time.time()
        stale_ids = [
            att_id for att_id, sess in list(self.sessions.items())
            if now - getattr(sess, "last_active_at", sess.started_at) > max_inactivity_seconds
        ]
        for att_id in stale_ids:
            logger.info("Auto-expiring inactive monitoring session for attempt %d", att_id)
            self.stop_session(att_id)

    def start_session(self, request: StartMonitoringRequest) -> StartMonitoringResponse:
        self.cleanup_stale_sessions()
        attempt_id = request.attemptId
        session = MonitoringSession(attempt_id, request.examId, request.token)
        self.sessions[attempt_id] = session

        if request.token:
            self.backend_client.register_token(attempt_id, request.token)

        self.rules_engine.get_or_create_state(attempt_id)

        logger.info("Started AI camera monitoring session for attempt %d", attempt_id)
        return StartMonitoringResponse(
            attemptId=attempt_id,
            status="ACTIVE",
            message="AI camera monitoring initialized successfully",
            startedAt=time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(session.started_at))
        )

    def _decode_frame(self, frame_base64: str) -> Optional[np.ndarray]:
        """
        Safely decodes base64 JPEG/PNG string to BGR OpenCV image.
        """
        try:
            if "," in frame_base64:
                # Strip data URL prefix (e.g. data:image/jpeg;base64,...)
                frame_base64 = frame_base64.split(",", 1)[1]

            image_bytes = base64.b64decode(frame_base64)
            nparr = np.frombuffer(image_bytes, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            return img
        except Exception as e:
            logger.warning("Failed to decode base64 frame: %s", e)
            return None

    async def process_frame(
        self,
        attempt_id: int,
        frame_base64: str,
        client_timestamp: Optional[float] = None
    ) -> FrameAnalysisResponse:
        """
        Processes a single video frame:
        1. Validates active session.
        2. Decodes frame.
        3. Detects faces.
        4. Estimates head pose.
        5. Estimates gaze direction.
        6. Evaluates temporal rules and cooldowns.
        7. Forwards qualified violations to Spring Boot.
        """
        now = time.time()

        # Validate or auto-register session for valid attempt
        session = self.sessions.get(attempt_id)
        if not session:
            session = MonitoringSession(attempt_id)
            self.sessions[attempt_id] = session
            self.rules_engine.get_or_create_state(attempt_id)
            logger.info("Auto-registered active monitoring session for attempt %d", attempt_id)
        elif not session.active:
            return FrameAnalysisResponse(
                attemptId=attempt_id,
                facesDetected=0,
                headPose=None,
                gaze=None,
                status="NORMAL",
                violationsTriggered=[]
            )
        session.last_active_at = now
        image = self._decode_frame(frame_base64)

        if image is None or image.size == 0:
            # Empty or invalid frame: treat as no face detected for this frame
            alerts = self.rules_engine.evaluate_frame(
                attempt_id=attempt_id,
                faces_count=0,
                head_pose=None,
                gaze=None,
                current_time=now
            )
            for alert in alerts:
                asyncio.create_task(self.backend_client.post_violation(alert))

            return FrameAnalysisResponse(
                attemptId=attempt_id,
                facesDetected=0,
                headPose=None,
                gaze=None,
                status="VIOLATION" if alerts else "NORMAL",
                violationsTriggered=alerts
            )

        h, w = image.shape[:2]

        # 1. Face detection
        detection_result = self.face_detector.detect(image)
        faces_count = detection_result.faces_count

        # 2. Head pose & Gaze estimation (for single face)
        head_pose = None
        gaze = None

        if faces_count == 1 and detection_result.landmarks_list:
            landmarks = detection_result.landmarks_list[0]
            blendshapes = detection_result.blendshapes_list[0] if detection_result.blendshapes_list else None

            head_pose = self.head_pose_detector.estimate_pose(landmarks, w, h)
            gaze = self.gaze_detector.estimate_gaze(landmarks, blendshapes)

        # 3. Rule evaluation
        alerts = self.rules_engine.evaluate_frame(
            attempt_id=attempt_id,
            faces_count=faces_count,
            head_pose=head_pose,
            gaze=gaze,
            current_time=now
        )

        # 4. Dispatch violations to Spring Boot asynchronously
        for alert in alerts:
            asyncio.create_task(self.backend_client.post_violation(alert))

        # Overall status
        status = "NORMAL"
        if alerts:
            status = "VIOLATION"
        elif faces_count != 1 or (head_pose and head_pose.direction != "CENTER") or (gaze and gaze.direction != "CENTER"):
            status = "SUSPICIOUS"

        return FrameAnalysisResponse(
            attemptId=attempt_id,
            facesDetected=faces_count,
            headPose=head_pose,
            gaze=gaze,
            status=status,
            violationsTriggered=alerts
        )

    def get_status(self, attempt_id: int) -> MonitoringStatusResponse:
        session = self.sessions.get(attempt_id)
        state = self.rules_engine.states.get(attempt_id)

        if not session or not state:
            return MonitoringStatusResponse(
                attemptId=attempt_id,
                active=False,
                framesProcessed=0,
                violationsLogged=0,
                currentStatus="NOT_INITIALIZED",
                uptimeSeconds=0.0
            )

        uptime = time.time() - session.started_at
        return MonitoringStatusResponse(
            attemptId=attempt_id,
            active=session.active,
            framesProcessed=state.frames_processed,
            violationsLogged=state.violations_logged,
            currentStatus="ACTIVE",
            uptimeSeconds=round(uptime, 1)
        )

    def stop_session(self, attempt_id: int) -> StopMonitoringResponse:
        session = self.sessions.pop(attempt_id, None)
        state = self.rules_engine.remove_state(attempt_id)
        self.backend_client.unregister_token(attempt_id)

        total_frames = state.frames_processed if state else 0
        total_violations = state.violations_logged if state else 0

        logger.info("Stopped AI monitoring for attempt %d. Frames: %d, Violations: %d", attempt_id, total_frames, total_violations)
        return StopMonitoringResponse(
            attemptId=attempt_id,
            status="STOPPED",
            totalFrames=total_frames,
            totalViolations=total_violations
        )
