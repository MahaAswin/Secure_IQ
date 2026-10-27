import axios from 'axios';

const AI_BASE_URL = (import.meta.env.VITE_AI_MONITORING_URL || 'http://localhost:8000').replace(/\/$/, '');

const aiClient = axios.create({
  baseURL: AI_BASE_URL,
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export interface HeadPoseResult {
  yaw: number;
  pitch: number;
  roll: number;
  direction: string;
}

export interface GazeResult {
  horizontalScore: number;
  verticalScore: number;
  direction: string;
}

export interface FrameAnalysisResult {
  attemptId: number;
  facesDetected: number;
  headPose: HeadPoseResult | null;
  gaze: GazeResult | null;
  status: 'NORMAL' | 'SUSPICIOUS' | 'VIOLATION';
  violationsTriggered: any[];
}

export interface MonitoringSessionStatus {
  attemptId: number;
  active: boolean;
  framesProcessed: number;
  violationsLogged: number;
  currentStatus: string;
  uptimeSeconds: number;
}

export const aiMonitoringService = {
  checkHealth: async () => {
    try {
      const res = await aiClient.get('/health');
      return res.data;
    } catch (err) {
      return null;
    }
  },

  startSession: async (attemptId: number, examId?: number, token?: string) => {
    const res = await aiClient.post('/monitoring/start', {
      attemptId,
      examId,
      token,
    });
    return res.data;
  },

  sendFrame: async (attemptId: number, frameBase64: string, signal?: AbortSignal): Promise<FrameAnalysisResult> => {
    const res = await aiClient.post<FrameAnalysisResult>('/monitoring/frame', {
      attemptId,
      frameBase64,
      timestamp: Date.now() / 1000,
    }, { signal });
    return res.data;
  },

  getStatus: async (attemptId: number): Promise<MonitoringSessionStatus> => {
    const res = await aiClient.get<MonitoringSessionStatus>(`/monitoring/status/${attemptId}`);
    return res.data;
  },

  stopSession: async (attemptId: number) => {
    try {
      const res = await aiClient.post('/monitoring/stop', { attemptId });
      return res.data;
    } catch {
      return null;
    }
  },
};

export default aiMonitoringService;
