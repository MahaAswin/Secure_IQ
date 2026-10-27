import api from './api';

export interface ViolationFilterParams {
  studentExamAttemptId?: number;
  sessionId?: number;
  studentId?: number;
  violationCode?: string;
  severity?: string;
  status?: string;
  page?: number;
  size?: number;
}

export const violationService = {
  getAll: async (params?: ViolationFilterParams) => {
    const res = await api.get('/v1/violations', { params });
    return res.data;
  },

  getById: async (id: string | number) => {
    const res = await api.get(`/v1/violations/${id}`);
    return res.data;
  },

  create: async (payload: { studentExamAttemptId: number; violationCode: string; violationType: string; severity: string; source: string; description?: string }) => {
    const res = await api.post('/v1/violations', payload);
    return res.data;
  },

  update: async (id: string | number, payload: any) => {
    const res = await api.put(`/v1/violations/${id}`, payload);
    return res.data;
  },

  getDashboard: async () => {
    const res = await api.get('/v1/violations/dashboard');
    return res.data;
  },
};

export const browserService = {
  connect: async (payload: { attemptCode: string; browserVersion?: string; ipAddress?: string; osDetails?: string }) => {
    const res = await api.post('/v1/browser/connect', payload);
    return res.data;
  },

  heartbeat: async (payload: { attemptCode: string }) => {
    const res = await api.post('/v1/browser/heartbeat', payload);
    return res.data;
  },

  logSecurityEvent: async (payload: { attemptCode: string; eventType: string; eventDetails?: string }) => {
    const res = await api.post('/v1/browser/security-event', payload);
    return res.data;
  },

  disconnect: async (payload: { attemptCode: string }) => {
    const res = await api.post('/v1/browser/disconnect', payload);
    return res.data;
  },

  getActive: async () => {
    const res = await api.get('/v1/browser/active');
    return res.data;
  },
};
