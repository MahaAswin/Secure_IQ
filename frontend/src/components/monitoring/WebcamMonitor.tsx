import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Camera, CheckCircle, AlertTriangle, XCircle, RefreshCw, Eye, UserCheck, Shield, Activity } from 'lucide-react';
import { aiMonitoringService, FrameAnalysisResult } from '@/services/aiMonitoringService';

export type CameraState =
  | 'CAMERA_INITIALIZING'
  | 'CAMERA_ACTIVE'
  | 'PERMISSION_DENIED'
  | 'DEVICE_UNAVAILABLE'
  | 'CAMERA_ERROR'
  | 'CAMERA_DISCONNECTED'
  | 'CAMERA_STOPPED';

export type DetectorState =
  | 'DETECTOR_INITIALIZING'
  | 'DETECTOR_READY'
  | 'DETECTOR_ERROR'
  | 'DETECTOR_STALLED'
  | 'MONITORING_ACTIVE'
  | 'MONITORING_STOPPED';

export interface WebcamMonitorStatus {
  cameraState: CameraState;
  detectorState: DetectorState;
  facesDetected: number;
  lastFrameProcessedAt: number | null;
}

interface WebcamMonitorProps {
  attemptId: number;
  examId?: number;
  token?: string;
  onViolationDetected?: (violation: any) => void;
  onStatusChange?: (status: WebcamMonitorStatus) => void;
  className?: string;
}

export function WebcamMonitor({
  attemptId,
  examId,
  token,
  onViolationDetected,
  onStatusChange,
  className = '',
}: WebcamMonitorProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const intervalRef = useRef<any>(null);
  const stallCheckIntervalRef = useRef<any>(null);
  const isProcessingRef = useRef(false);

  // Lifecycle guards to eliminate asynchronous race conditions and orphaned streams
  const isMountedRef = useRef<boolean>(true);
  const runIdRef = useRef<number>(0);
  const abortControllerRef = useRef<AbortController | null>(null);

  // States
  const [cameraState, setCameraState] = useState<CameraState>('CAMERA_INITIALIZING');
  const [detectorState, setDetectorState] = useState<DetectorState>('DETECTOR_INITIALIZING');
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [detectorError, setDetectorError] = useState<string>('');
  const [facesDetected, setFacesDetected] = useState<number>(0);
  const [headDirection, setHeadDirection] = useState<string>('CENTER');
  const [gazeDirection, setGazeDirection] = useState<string>('CENTER');
  const [riskStatus, setRiskStatus] = useState<'NORMAL' | 'SUSPICIOUS' | 'VIOLATION'>('NORMAL');
  const [violationsCount, setViolationsCount] = useState<number>(0);
  const [lastFrameTime, setLastFrameTime] = useState<number | null>(null);

  // Propagate status changes up to parent
  useEffect(() => {
    if (onStatusChange) {
      onStatusChange({
        cameraState,
        detectorState,
        facesDetected,
        lastFrameProcessedAt: lastFrameTime,
      });
    }
  }, [cameraState, detectorState, facesDetected, lastFrameTime, onStatusChange]);

  // Reliable stop: releases hardware tracks and cancels pending work
  const stopCamera = useCallback(() => {
    isMountedRef.current = false;
    runIdRef.current += 1;

    // 1. Abort any pending network frame request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }

    // 2. Clear capture and stall intervals
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    if (stallCheckIntervalRef.current) {
      clearInterval(stallCheckIntervalRef.current);
      stallCheckIntervalRef.current = null;
    }
    isProcessingRef.current = false;

    // 3. Stop every track on the media stream
    if (streamRef.current) {
      try {
        streamRef.current.getTracks().forEach((track) => {
          track.onended = null;
          track.stop();
        });
      } catch (err) {
        console.warn('Error stopping camera tracks:', err);
      }
      streamRef.current = null;
    }

    // 4. Detach video element and release hardware handle
    if (videoRef.current) {
      try {
        videoRef.current.pause();
        videoRef.current.srcObject = null;
      } catch {}
    }

    // 5. Clear global fallback
    if (typeof window !== 'undefined' && (window as any).__secureIqActiveMediaStream) {
      try {
        const globalStream = (window as any).__secureIqActiveMediaStream as MediaStream;
        globalStream.getTracks().forEach((track) => {
          track.onended = null;
          track.stop();
        });
        (window as any).__secureIqActiveMediaStream = null;
      } catch {}
    }

    // 6. Notify Python AI monitoring service (best-effort, non-blocking)
    if (attemptId && attemptId > 0) {
      aiMonitoringService.stopSession(attemptId).catch(() => {});
    }

    setCameraState('CAMERA_STOPPED');
    setDetectorState('MONITORING_STOPPED');
  }, [attemptId]);

  // Camera Initialization & Preview Attachment
  const startCamera = useCallback(async () => {
    if (!attemptId || attemptId <= 0) {
      setCameraState('CAMERA_STOPPED');
      setDetectorState('MONITORING_STOPPED');
      return;
    }

    isMountedRef.current = true;
    const currentRunId = ++runIdRef.current;

    setCameraState('CAMERA_INITIALIZING');
    setDetectorState('DETECTOR_INITIALIZING');
    setErrorMessage('');
    setDetectorError('');

    // Stop and clean up any pre-existing stream before acquiring a new one
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => {
        track.onended = null;
        track.stop();
      });
      streamRef.current = null;
    }

    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        if (isMountedRef.current && runIdRef.current === currentRunId) {
          setCameraState('DEVICE_UNAVAILABLE');
          setErrorMessage('Camera access is not supported by this browser.');
        }
        return;
      }

      // Step 1: Acquire real media stream from user webcam
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 640 },
          height: { ideal: 480 },
          facingMode: 'user',
        },
        audio: false,
      });

      // RACE CONDITION CHECK:
      // If the component unmounted while awaiting getUserMedia, terminate all acquired tracks immediately!
      if (!isMountedRef.current || runIdRef.current !== currentRunId) {
        stream.getTracks().forEach((track) => {
          track.onended = null;
          track.stop();
        });
        return;
      }

      const videoTrack = stream.getVideoTracks()[0];
      if (!videoTrack || videoTrack.readyState !== 'live' || !videoTrack.enabled) {
        throw new Error('No live video track available from camera device.');
      }

      streamRef.current = stream;
      if (typeof window !== 'undefined') {
        (window as any).__secureIqActiveMediaStream = stream;
      }

      // Step 2: Attach stream to video element and start playback
      const video = videoRef.current;
      if (video) {
        video.srcObject = stream;
        video.muted = true;
        video.playsInline = true;
        video.autoplay = true;

        try {
          await video.play();
        } catch (err) {
          console.warn('Initial video play caught:', err);
        }

        // Wait up to 1.5s for video dimensions to stabilize
        await new Promise<void>((resolve) => {
          let timeoutId: any = null;
          const done = () => {
            if (timeoutId) clearTimeout(timeoutId);
            video.removeEventListener('loadedmetadata', done);
            video.removeEventListener('loadeddata', done);
            video.removeEventListener('playing', done);
            resolve();
          };
          timeoutId = setTimeout(done, 1500);

          if (video.videoWidth > 0 && video.readyState >= 1) {
            done();
          } else {
            video.addEventListener('loadedmetadata', done);
            video.addEventListener('loadeddata', done);
            video.addEventListener('playing', done);
          }
        });

        if (!isMountedRef.current || runIdRef.current !== currentRunId) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
      }

      // Step 3: Listen for hardware disconnection
      videoTrack.onended = () => {
        if (isMountedRef.current && runIdRef.current === currentRunId) {
          setCameraState('CAMERA_DISCONNECTED');
          setErrorMessage('Camera device was disconnected.');
        }
      };

      // Real live camera stream is confirmed active!
      if (isMountedRef.current && runIdRef.current === currentRunId) {
        setCameraState('CAMERA_ACTIVE');
      }

      // Step 4: Connect Python AI Detector
      try {
        const health = await aiMonitoringService.checkHealth();
        if (!isMountedRef.current || runIdRef.current !== currentRunId) return;

        if (!health) {
          setDetectorState('DETECTOR_ERROR');
          setDetectorError('AI Detector service offline on port 8000');
        } else {
          await aiMonitoringService.startSession(attemptId, examId, token);
          if (isMountedRef.current && runIdRef.current === currentRunId) {
            setDetectorState('DETECTOR_READY');
          }
        }
      } catch (err: any) {
        if (isMountedRef.current && runIdRef.current === currentRunId) {
          setDetectorState('DETECTOR_ERROR');
          setDetectorError('Could not connect to AI monitoring microservice');
        }
      }
    } catch (err: any) {
      if (!isMountedRef.current || runIdRef.current !== currentRunId) {
        return;
      }
      console.error('Camera initialization error:', err);
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        setCameraState('PERMISSION_DENIED');
        setErrorMessage('Camera permission was denied. Please allow camera access in browser permissions and retry.');
      } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
        setCameraState('DEVICE_UNAVAILABLE');
        setErrorMessage('No camera device was detected on your computer.');
      } else {
        setCameraState('CAMERA_ERROR');
        setErrorMessage(err.message || 'Unable to access camera.');
      }
    }
  }, [attemptId, examId, token]);

  // Frame Capture and AI Detection Pipeline
  useEffect(() => {
    if (cameraState !== 'CAMERA_ACTIVE') {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      return;
    }

    let consecutiveFailures = 0;

    const captureAndAnalyze = async () => {
      if (!isMountedRef.current || isProcessingRef.current || !videoRef.current || !canvasRef.current) {
        return;
      }

      const video = videoRef.current;
      const canvas = canvasRef.current;

      // Ensure video is actively receiving frames with valid dimensions
      if (video.readyState < 2 || video.videoWidth === 0 || video.videoHeight === 0) {
        return;
      }

      isProcessingRef.current = true;
      abortControllerRef.current = new AbortController();

      try {
        canvas.width = 320;
        canvas.height = 240;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        const base64Data = canvas.toDataURL('image/jpeg', 0.65);

        const result: FrameAnalysisResult = await aiMonitoringService.sendFrame(
          attemptId,
          base64Data,
          abortControllerRef.current.signal
        );

        if (!isMountedRef.current) return;

        consecutiveFailures = 0;
        setDetectorState('MONITORING_ACTIVE');
        setLastFrameTime(Date.now());
        setDetectorError('');

        setFacesDetected(result.facesDetected);
        setHeadDirection(result.headPose?.direction || 'CENTER');
        setGazeDirection(result.gaze?.direction || 'CENTER');
        setRiskStatus(result.status);

        if (result.violationsTriggered && result.violationsTriggered.length > 0) {
          setViolationsCount((c) => c + result.violationsTriggered.length);
          if (onViolationDetected) {
            result.violationsTriggered.forEach((v) => onViolationDetected(v));
          }
        }
      } catch (err: any) {
        if (!isMountedRef.current) return;
        if (err.name !== 'CanceledError' && err.name !== 'AbortError') {
          consecutiveFailures += 1;
          if (consecutiveFailures >= 3) {
            setDetectorState('DETECTOR_ERROR');
            setDetectorError('AI service connection interrupted');
          }
        }
      } finally {
        isProcessingRef.current = false;
        abortControllerRef.current = null;
      }
    };

    // Process frame every 1.5 seconds
    intervalRef.current = setInterval(captureAndAnalyze, 1500);

    // Initial frame capture after 500ms
    const initialTimer = setTimeout(captureAndAnalyze, 500);

    return () => {
      clearTimeout(initialTimer);
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    };
  }, [cameraState, attemptId, onViolationDetected]);

  // Stall detector watchdog
  useEffect(() => {
    if (cameraState !== 'CAMERA_ACTIVE' || detectorState !== 'MONITORING_ACTIVE') {
      if (stallCheckIntervalRef.current) {
        clearInterval(stallCheckIntervalRef.current);
        stallCheckIntervalRef.current = null;
      }
      return;
    }

    stallCheckIntervalRef.current = setInterval(() => {
      if (lastFrameTime && Date.now() - lastFrameTime > 6000) {
        setDetectorState('DETECTOR_STALLED');
      }
    }, 3000);

    return () => {
      if (stallCheckIntervalRef.current) {
        clearInterval(stallCheckIntervalRef.current);
        stallCheckIntervalRef.current = null;
      }
    };
  }, [cameraState, detectorState, lastFrameTime]);

  // Mount & unmount lifecycle
  useEffect(() => {
    isMountedRef.current = true;
    startCamera();

    const handleWindowExit = () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => {
          try {
            track.onended = null;
            track.stop();
          } catch {}
        });
        streamRef.current = null;
      }
      if (videoRef.current) {
        videoRef.current.srcObject = null;
      }
    };

    window.addEventListener('pagehide', handleWindowExit);
    window.addEventListener('beforeunload', handleWindowExit);

    return () => {
      window.removeEventListener('pagehide', handleWindowExit);
      window.removeEventListener('beforeunload', handleWindowExit);
      stopCamera();
    };
  }, [startCamera, stopCamera]);

  return (
    <div className={`space-y-3 ${className}`}>
      {/* Hidden processing canvas */}
      <canvas ref={canvasRef} className="hidden" />

      {/* Video Preview Container */}
      <div className="relative aspect-video rounded-lg bg-dark-950 overflow-hidden border border-dark-700 flex items-center justify-center">
        {/* Video element is permanently attached to DOM so ref is always available */}
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className={`w-full h-full object-cover mirror transition-opacity duration-300 ${
            cameraState === 'CAMERA_ACTIVE' ? 'opacity-100' : 'opacity-0 absolute inset-0 pointer-events-none'
          }`}
          style={{ transform: 'scaleX(-1)' }}
        />

        {/* Camera Initializing Overlay */}
        {cameraState === 'CAMERA_INITIALIZING' && (
          <div className="text-center p-3 z-10">
            <RefreshCw className="h-6 w-6 text-primary animate-spin mx-auto mb-2" />
            <p className="text-xs font-semibold text-white">Connecting Camera...</p>
            <p className="text-[10px] text-dark-400 mt-0.5">Please allow camera permissions if prompted</p>
          </div>
        )}

        {/* Camera Stopped Overlay */}
        {cameraState === 'CAMERA_STOPPED' && (
          <div className="text-center p-3 z-10">
            <Camera className="h-6 w-6 text-dark-500 mx-auto mb-1.5" />
            <p className="text-xs text-dark-300 font-medium">Monitoring Concluded</p>
            <p className="text-[10px] text-dark-500">Camera stream released</p>
          </div>
        )}

        {/* Camera Permission Denied Overlay */}
        {cameraState === 'PERMISSION_DENIED' && (
          <div className="text-center p-3 z-10 max-w-[200px]">
            <XCircle className="h-6 w-6 text-danger mx-auto mb-1.5" />
            <p className="text-xs text-danger font-semibold">Permission Denied</p>
            <p className="text-[10px] text-dark-400 mt-1">
              Click the camera icon in your browser URL bar to allow access.
            </p>
            <button
              onClick={startCamera}
              className="mt-2 text-[10px] font-medium px-2.5 py-1 rounded bg-dark-800 text-white hover:bg-dark-700 border border-dark-600 transition-colors"
            >
              Retry Camera
            </button>
          </div>
        )}

        {/* Camera Device Unavailable / Error Overlay */}
        {(cameraState === 'DEVICE_UNAVAILABLE' ||
          cameraState === 'CAMERA_ERROR' ||
          cameraState === 'CAMERA_DISCONNECTED') && (
          <div className="text-center p-3 z-10 max-w-[200px]">
            <AlertTriangle className="h-6 w-6 text-warning mx-auto mb-1.5" />
            <p className="text-xs text-warning font-semibold">
              {cameraState === 'DEVICE_UNAVAILABLE'
                ? 'No Camera Found'
                : cameraState === 'CAMERA_DISCONNECTED'
                ? 'Camera Disconnected'
                : 'Camera Error'}
            </p>
            <p className="text-[10px] text-dark-400 mt-1">
              {errorMessage || 'Check webcam connection and try again.'}
            </p>
            <button
              onClick={startCamera}
              className="mt-2 text-[10px] font-medium px-2.5 py-1 rounded bg-dark-800 text-white hover:bg-dark-700 border border-dark-600 transition-colors"
            >
              Reconnect
            </button>
          </div>
        )}

        {/* Live Top-Right Status Badge */}
        {cameraState === 'CAMERA_ACTIVE' && (
          <div className="absolute top-2 right-2 flex items-center gap-1.5 bg-black/70 backdrop-blur-md px-2 py-0.5 rounded-full text-[10px] font-medium shadow-sm z-10">
            {detectorState === 'MONITORING_ACTIVE' && (
              <>
                <span className="h-1.5 w-1.5 rounded-full bg-green-400 animate-pulse" />
                <span className="text-green-400">AI Active</span>
              </>
            )}
            {detectorState === 'DETECTOR_READY' && (
              <>
                <span className="h-1.5 w-1.5 rounded-full bg-blue-400 animate-pulse" />
                <span className="text-blue-400">AI Ready</span>
              </>
            )}
            {detectorState === 'DETECTOR_INITIALIZING' && (
              <>
                <RefreshCw className="h-2.5 w-2.5 text-yellow-400 animate-spin" />
                <span className="text-yellow-400">AI Syncing</span>
              </>
            )}
            {detectorState === 'DETECTOR_STALLED' && (
              <>
                <span className="h-1.5 w-1.5 rounded-full bg-warning animate-ping" />
                <span className="text-warning">AI Stalled</span>
              </>
            )}
            {detectorState === 'DETECTOR_ERROR' && (
              <>
                <span className="h-1.5 w-1.5 rounded-full bg-dark-500" />
                <span className="text-dark-300">Camera Only</span>
              </>
            )}
          </div>
        )}
      </div>

      {/* Detector Warning Banner (if detector service is offline but camera works) */}
      {detectorError && cameraState === 'CAMERA_ACTIVE' && (
        <div className="flex items-center gap-1.5 p-2 rounded bg-amber-500/10 border border-amber-500/20 text-[10px] text-amber-400">
          <AlertTriangle className="h-3.5 w-3.5 flex-shrink-0" />
          <span>{detectorError}</span>
        </div>
      )}

      {/* Live AI Behavioral Indicators */}
      <div className="space-y-1.5">
        {/* Face status */}
        <div className="flex items-center justify-between text-xs py-0.5">
          <div className="flex items-center gap-1.5">
            {facesDetected === 1 ? (
              <UserCheck className="h-3.5 w-3.5 text-green-400" />
            ) : (
              <AlertTriangle className="h-3.5 w-3.5 text-warning" />
            )}
            <span className="text-dark-300">Face Status</span>
          </div>
          <span className={`text-[11px] font-medium ${facesDetected === 1 ? 'text-green-400' : 'text-warning'}`}>
            {facesDetected === 0 ? 'Face Not Visible' : facesDetected === 1 ? '1 Face Detected' : `${facesDetected} Faces`}
          </span>
        </div>

        {/* Head Orientation */}
        <div className="flex items-center justify-between text-xs py-0.5">
          <div className="flex items-center gap-1.5">
            {headDirection === 'CENTER' ? (
              <CheckCircle className="h-3.5 w-3.5 text-green-400" />
            ) : (
              <AlertTriangle className="h-3.5 w-3.5 text-warning" />
            )}
            <span className="text-dark-300">Head Pose</span>
          </div>
          <span className={`text-[11px] font-medium ${headDirection === 'CENTER' ? 'text-green-400' : 'text-warning'}`}>
            {headDirection === 'CENTER' ? 'Facing Screen' : `Looking ${headDirection}`}
          </span>
        </div>

        {/* Eye Gaze */}
        <div className="flex items-center justify-between text-xs py-0.5">
          <div className="flex items-center gap-1.5">
            <Eye className="h-3.5 w-3.5 text-primary" />
            <span className="text-dark-300">Eye Gaze</span>
          </div>
          <span className={`text-[11px] font-medium ${gazeDirection === 'CENTER' ? 'text-green-400' : 'text-dark-300'}`}>
            {gazeDirection === 'CENTER' ? 'Centered' : gazeDirection}
          </span>
        </div>
      </div>

      {/* Session Proctor Integrity Bar */}
      <div className="p-2.5 rounded-lg bg-dark-800 border border-dark-700 space-y-1.5">
        <div className="flex justify-between text-[10px]">
          <span className="text-dark-400">Proctor Integrity</span>
          <span
            className={`font-bold ${
              riskStatus === 'NORMAL' ? 'text-green-400' : riskStatus === 'SUSPICIOUS' ? 'text-warning' : 'text-danger'
            }`}
          >
            {riskStatus === 'NORMAL' ? 'Optimal' : riskStatus === 'SUSPICIOUS' ? 'Under Review' : 'Flagged'}
          </span>
        </div>
        <div className="h-1.5 rounded-full bg-dark-700 overflow-hidden">
          <div
            className={`h-full transition-all duration-300 rounded-full ${
              riskStatus === 'NORMAL'
                ? 'bg-green-400 w-full'
                : riskStatus === 'SUSPICIOUS'
                ? 'bg-warning w-2/3'
                : 'bg-danger w-1/3'
            }`}
          />
        </div>

        <div className="flex justify-between items-center text-[9px] text-dark-500 pt-0.5">
          <span>AI Engine</span>
          <span className="flex items-center gap-1 font-mono">
            {lastFrameTime ? (
              <>
                <span className="h-1 w-1 rounded-full bg-green-400" />
                <span>{((Date.now() - lastFrameTime) / 1000).toFixed(1)}s ago</span>
              </>
            ) : (
              <span>Syncing</span>
            )}
          </span>
        </div>
      </div>
    </div>
  );
}
