"""
SecureIQ AI Monitoring Configuration
Centralized settings with environment variable support.
"""
from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional
import os


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # Service configuration
    SERVICE_NAME: str = "SecureIQ AI Camera Monitoring Service"
    SERVICE_VERSION: str = "1.0.0"
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    DEBUG: bool = False

    # Spring Boot Integration
    SPRING_BOOT_API_BASE_URL: str = "http://localhost:8080"
    VIOLATIONS_ENDPOINT: str = "/api/v1/violations"

    # Monitoring Activation
    MONITORING_ENABLED: bool = True

    # Face Detection Thresholds
    FACE_ABSENCE_THRESHOLD_SECONDS: float = 3.0  # Seconds before NO_FACE triggers
    MULTIPLE_FACE_THRESHOLD_SECONDS: float = 2.0  # Seconds before MULTIPLE_PERSON triggers
    MIN_FACE_DETECTION_CONFIDENCE: float = 0.5

    # Head Pose Thresholds (Degrees)
    HEAD_YAW_THRESHOLD: float = 25.0    # Left/Right turn threshold
    HEAD_PITCH_UP_THRESHOLD: float = 20.0  # Upward threshold
    HEAD_PITCH_DOWN_THRESHOLD: float = 25.0  # Downward threshold
    LOOK_AWAY_THRESHOLD_SECONDS: float = 3.0  # Seconds before sustained look-away triggers

    # Eye Movement & Gaze Thresholds
    GAZE_HORIZONTAL_THRESHOLD: float = 0.55  # Blendshape / geometric deviation threshold
    GAZE_AWAY_THRESHOLD_SECONDS: float = 3.5

    # False-Positive Reduction & Cooldown
    DETECTION_COOLDOWN_SECONDS: float = 10.0  # Cooldown between repeat alerts of same type
    FRAME_PROCESSING_INTERVAL_MS: int = 1000   # Expected client send interval

    # Model Asset Path
    MODEL_PATH: str = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "models",
        "face_landmarker.task"
    )


settings = Settings()
