"""
Data schemas for SecureIQ AI Monitoring.
"""
from pydantic import BaseModel, Field
from typing import Optional, List


class StartMonitoringRequest(BaseModel):
    attemptId: int = Field(..., description="Student Exam Attempt ID")
    examId: Optional[int] = Field(None, description="Exam ID")
    token: Optional[str] = Field(None, description="Student JWT authorization token")


class StartMonitoringResponse(BaseModel):
    attemptId: int
    status: str
    message: str
    startedAt: str


class FrameAnalysisRequest(BaseModel):
    attemptId: int = Field(..., description="Student Exam Attempt ID")
    frameBase64: str = Field(..., description="Base64 encoded JPEG/PNG frame")
    timestamp: Optional[float] = Field(None, description="Client frame timestamp (epoch seconds)")


class HeadPoseData(BaseModel):
    yaw: float = Field(..., description="Yaw angle in degrees (negative=left, positive=right)")
    pitch: float = Field(..., description="Pitch angle in degrees (negative=down, positive=up)")
    roll: float = Field(..., description="Roll angle in degrees")
    direction: str = Field(..., description="CENTER, LEFT, RIGHT, UP, DOWN")


class GazeData(BaseModel):
    horizontalScore: float = Field(..., description="Horizontal gaze offset (-1.0 to 1.0)")
    verticalScore: float = Field(..., description="Vertical gaze offset (-1.0 to 1.0)")
    direction: str = Field(..., description="CENTER, LEFT, RIGHT, UP, DOWN")


class ViolationAlert(BaseModel):
    violationCode: str
    studentExamAttemptId: int
    violationType: str  # NO_FACE, MULTIPLE_PERSON, HEAD_POSE, EYE_MOVEMENT
    severity: str       # LOW, MEDIUM, HIGH, CRITICAL
    source: str = "AI_ENGINE"
    description: str
    confidenceScore: float
    detectedAt: str     # ISO format
    actionTaken: str = "LOGGED_BY_AI"


class FrameAnalysisResponse(BaseModel):
    attemptId: int
    facesDetected: int
    headPose: Optional[HeadPoseData] = None
    gaze: Optional[GazeData] = None
    status: str  # NORMAL, SUSPICIOUS, VIOLATION
    violationsTriggered: List[ViolationAlert] = []


class MonitoringStatusResponse(BaseModel):
    attemptId: int
    active: bool
    framesProcessed: int
    violationsLogged: int
    currentStatus: str
    uptimeSeconds: float


class StopMonitoringRequest(BaseModel):
    attemptId: int


class StopMonitoringResponse(BaseModel):
    attemptId: int
    status: str
    totalFrames: int
    totalViolations: int
