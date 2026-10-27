"""
SecureIQ AI Camera Monitoring Service Main Application.
"""
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.api.monitoring import router as monitoring_router

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("secureiq-ai-monitoring")

from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting %s v%s", settings.SERVICE_NAME, settings.SERVICE_VERSION)
    logger.info("Spring Boot Base URL: %s", settings.SPRING_BOOT_API_BASE_URL)
    logger.info("Model path: %s", settings.MODEL_PATH)
    yield
    logger.info("Shutting down %s", settings.SERVICE_NAME)

app = FastAPI(
    title=settings.SERVICE_NAME,
    version=settings.SERVICE_VERSION,
    description="Real-time AI camera monitoring service for SecureIQ examination proctoring.",
    lifespan=lifespan
)

# Enable CORS for React frontend (e.g. http://localhost:5173) and local development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(monitoring_router)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG
    )
