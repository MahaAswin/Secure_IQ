"""
Backend HTTP Client for forwarding detected violations to Spring Boot SecureIQ API.
"""
import logging
import httpx
from typing import Optional, Dict
from app.config import settings
from app.schemas.monitoring import ViolationAlert

logger = logging.getLogger(__name__)


class BackendClient:
    def __init__(self, base_url: str = settings.SPRING_BOOT_API_BASE_URL):
        self.base_url = base_url.rstrip("/")
        self.violations_url = f"{self.base_url}{settings.VIOLATIONS_ENDPOINT}"
        # Map of attempt_id -> jwt_token
        self.session_tokens: Dict[int, str] = {}

    def register_token(self, attempt_id: int, token: Optional[str]):
        if token:
            # Strip Bearer prefix if provided
            clean_token = token.replace("Bearer ", "").strip()
            self.session_tokens[attempt_id] = clean_token

    def unregister_token(self, attempt_id: int):
        self.session_tokens.pop(attempt_id, None)

    async def post_violation(self, alert: ViolationAlert) -> bool:
        """
        Sends a qualified violation to Spring Boot POST /api/v1/violations.
        """
        token = self.session_tokens.get(alert.studentExamAttemptId)
        headers = {
            "Content-Type": "application/json"
        }
        if token:
            headers["Authorization"] = f"Bearer {token}"

        payload = {
            "violationCode": alert.violationCode,
            "studentExamAttemptId": alert.studentExamAttemptId,
            "violationType": alert.violationType,
            "severity": alert.severity,
            "source": alert.source,
            "description": alert.description,
            "confidenceScore": alert.confidenceScore,
            "detectedAt": alert.detectedAt,
            "actionTaken": alert.actionTaken
        }

        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.post(
                    self.violations_url,
                    json=payload,
                    headers=headers
                )

                if response.status_code in (200, 201):
                    logger.info(
                        "Violation %s successfully recorded in Spring Boot for attempt %d",
                        alert.violationCode,
                        alert.studentExamAttemptId
                    )
                    return True
                else:
                    logger.warning(
                        "Spring Boot rejected violation (%d): %s",
                        response.status_code,
                        response.text
                    )
                    return False
        except Exception as e:
            logger.error("Failed to forward violation to Spring Boot: %s", e)
            return False
