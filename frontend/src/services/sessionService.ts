import api from './api';

export interface ExamSessionFilterParams {
  examId?: number;
  facultyId?: number;
  status?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface ExamSessionCreatePayload {
  sessionCode: string;
  sessionName: string;
  examId: number;
  facultyId: number;
  startDateTime: string;
  endDateTime: string;
  venue?: string;
  instructions?: string;
  status?: string;
}

export const sessionService = {
  getAll: async (params?: ExamSessionFilterParams) => {
    const res = await api.get('/v1/exam-sessions', { params });
    return res.data;
  },

  getById: async (id: string | number) => {
    const res = await api.get(`/v1/exam-sessions/${id}`);
    return res.data;
  },

  create: async (payload: ExamSessionCreatePayload) => {
    const res = await api.post('/v1/exam-sessions', payload);
    return res.data;
  },

  update: async (id: string | number, payload: ExamSessionCreatePayload) => {
    const res = await api.put(`/v1/exam-sessions/${id}`, payload);
    return res.data;
  },

  patch: async (id: string | number, payload: Partial<ExamSessionCreatePayload>) => {
    const res = await api.patch(`/v1/exam-sessions/${id}`, payload);
    return res.data;
  },

  delete: async (id: string | number) => {
    const res = await api.delete(`/v1/exam-sessions/${id}`);
    return res.data;
  },

  assignStudents: async (sessionId: string | number, studentIds: number[]) => {
    const res = await api.post(`/v1/exam-sessions/${sessionId}/assign-students`, { studentIds });
    return res.data;
  },

  getAssignedStudents: async (sessionId: string | number) => {
    const res = await api.get(`/v1/exam-sessions/${sessionId}/students`);
    return res.data;
  },

  getLive: async () => {
    const res = await api.get('/v1/exam-sessions/live');
    return res.data;
  },
};
