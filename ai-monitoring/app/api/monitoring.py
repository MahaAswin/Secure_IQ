"""
FastAPI Monitoring API Router.
"""
from fastapi import APIRouter, HTTPException, Depends
from typing import Dict, Any

from app.schemas.monitoring import (
    StartMonitoringRequest,
    StartMonitoringResponse,
    FrameAnalysisRequest,
    FrameAnalysisResponse,
    MonitoringStatusResponse,
    StopMonitoringRequest,
    StopMonitoringResponse,
)
from app.services.monitoring_service import MonitoringService
from app.config import settings

router = APIRouter()

# Global monitoring service singleton
_monitoring_service: MonitoringService = None


def get_monitoring_service() -> MonitoringService:
    global _monitoring_service
    if _monitoring_service is None:
        _monitoring_service = MonitoringService()
    return _monitoring_service


@router.get("/health", summary="Service Health Check")
async def health_check():
    service = get_monitoring_service()
    model_loaded = service.face_detector.landmarker is not None
    return {
        "status": "UP" if model_loaded else "DEGRADED",
        "service": settings.SERVICE_NAME,
        "version": settings.SERVICE_VERSION,
        "detector": {
            "modelLoaded": model_loaded,
            "minConfidence": settings.MIN_FACE_DETECTION_CONFIDENCE,
            "backendUrl": settings.SPRING_BOOT_API_BASE_URL
        }
    }


@router.post("/monitoring/start", response_model=StartMonitoringResponse, summary="Start Monitoring Session")
async def start_monitoring(
    request: StartMonitoringRequest,
    service: MonitoringService = Depends(get_monitoring_service)
):
    if request.attemptId <= 0:
        raise HTTPException(status_code=400, detail="Invalid attempt ID")
    return service.start_session(request)


@router.post("/monitoring/frame", response_model=FrameAnalysisResponse, summary="Process Camera Frame")
async def analyze_frame(
    request: FrameAnalysisRequest,
    service: MonitoringService = Depends(get_monitoring_service)
):
    if request.attemptId <= 0:
        raise HTTPException(status_code=400, detail="Invalid attempt ID")
    if not request.frameBase64:
        raise HTTPException(status_code=400, detail="Frame data cannot be empty")

    return await service.process_frame(
        attempt_id=request.attemptId,
        frame_base64=request.frameBase64,
        client_timestamp=request.timestamp
    )


@router.get("/monitoring/status/{attempt_id}", response_model=MonitoringStatusResponse, summary="Get Monitoring Session Status")
async def get_monitoring_status(
    attempt_id: int,
    service: MonitoringService = Depends(get_monitoring_service)
):
    if attempt_id <= 0:
        raise HTTPException(status_code=400, detail="Invalid attempt ID")
    return service.get_status(attempt_id)


@router.post("/monitoring/stop", response_model=StopMonitoringResponse, summary="Stop Monitoring Session")
async def stop_monitoring(
    request: StopMonitoringRequest,
    service: MonitoringService = Depends(get_monitoring_service)
):
    if request.attemptId <= 0:
        raise HTTPException(status_code=400, detail="Invalid attempt ID")
    return service.stop_session(request.attemptId)
