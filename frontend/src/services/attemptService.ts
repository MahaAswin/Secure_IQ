import api from './api';

export interface StartAttemptPayload {
  examSessionId?: number;
  examId?: number;
  resume?: boolean;
}

export interface SubmitAttemptPayload {
  attemptId: number;
  obtainedMarks?: number;
  autoSubmitted?: boolean;
}

export const attemptService = {
  start: async (payload: StartAttemptPayload) => {
    const res = await api.post('/v1/attempts/start', payload);
    return res.data;
  },

  getBySession: async (sessionId: string | number) => {
    const res = await api.get(`/v1/attempts/session/${sessionId}`);
    return res.data;
  },

  submit: async (payload: SubmitAttemptPayload) => {
    const res = await api.patch('/v1/attempts/submit', payload);
    return res.data;
  },

  getHistory: async () => {
    const res = await api.get('/v1/attempts/history');
    return res.data;
  },

  getSummary: async () => {
    const res = await api.get('/v1/attempts/summary');
    return res.data;
  },

  getStudentAttempts: async (params?: { examSessionId?: number; studentId?: number; page?: number; size?: number }) => {
    const res = await api.get('/v1/attempts/student', { params });
    return res.data;
  },

  getAll: async (params?: { examSessionId?: number; status?: string; page?: number; size?: number }) => {
    const res = await api.get('/v1/attempts', { params });
    return res.data;
  },

  getById: async (id: string | number) => {
    const res = await api.get(`/v1/attempts/${id}`);
    return res.data;
  },
};
