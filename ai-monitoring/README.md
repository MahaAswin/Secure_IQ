# SecureIQ AI Camera Monitoring Service

Real-time CPU-optimized AI camera monitoring microservice for SecureIQ examination proctoring. Built using FastAPI, OpenCV, and Google MediaPipe Face Landmarker with Face Blendshapes.

## Architecture

```
Student Exam Page (React Webcam)
    ↓ (Periodic Frame Capture via HTTP POST)
Python AI Monitoring Service (FastAPI :8000)
    ↓ (Face Detection, Head Pose Estimation, Eye Gaze Analysis, Rules & Cooldowns)
Qualified Violation Event
    ↓ (HTTP POST /api/v1/violations with Student JWT Bearer token)
Spring Boot Backend (:8080)
    ↓
PostgreSQL Database
    ↓
Faculty Live Monitoring Dashboard (:5173)
```

## Features

1. **Face Presence Detection**:
   - `NO_FACE`: Detects when student leaves the camera view for >= 3.0 seconds (configurable).
   - `MULTIPLE_PERSON`: Detects multiple individuals in the webcam frame for >= 2.0 seconds.

2. **Head Pose Estimation**:
   - `HEAD_POSE`: Solves Perspective-n-Point (`cv2.solvePnP`) using 6 canonical 3D facial landmarks.
   - Detects sustained left/right turn (|yaw| > 25°) or up/down tilt (|pitch| > 20°/25°) for >= 3.0 seconds.

3. **Eye Gaze & Movement Analysis**:
   - `EYE_MOVEMENT`: Uses 52 MediaPipe face blendshapes (`eyeLookOutLeft`, `eyeLookInRight`, etc.) and iris landmarks.
   - Analyzes eye gaze independently from head orientation to detect looking away without moving the head.

4. **False-Positive Reduction & Cooldowns**:
   - Consecutive duration thresholds prevent brief glances or blinks from triggering false alerts.
   - Configurable cooldown timer (default: 10s) prevents repeated alerts for continuing behavior.
   - State and timers reset immediately when student returns to normal orientation.

5. **Privacy & Security**:
   - Does not store raw video frames.
   - No biometric identity matching or audio capture.
   - Enforces student authorization when dispatching to Spring Boot.

## Prerequisites

- Python 3.10+ (64-bit)
- MediaPipe Face Landmarker model (`models/face_landmarker.task`)

## Setup Instructions

### 1. Create Virtual Environment
```powershell
cd ai-monitoring
py -3.10 -m venv venv
```

### 2. Activate & Install Dependencies
```powershell
.\venv\Scripts\activate
pip install -r requirements.txt
```

### 3. Download MediaPipe Model (if not already present)
```powershell
python -c "import urllib.request, os; os.makedirs('models', exist_ok=True); urllib.request.urlretrieve('https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task', 'models/face_landmarker.task')"
```

### 4. Configure Environment
Copy `.env.example` to `.env`:
```powershell
cp .env.example .env
```

### 5. Run Tests
```powershell
.\venv\Scripts\python.exe -m pytest
```

### 6. Start Service
```powershell
.\venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
The service will start on `http://localhost:8000` with Swagger documentation at `http://localhost:8000/docs`.
