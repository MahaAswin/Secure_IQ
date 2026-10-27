import api from './api';

export interface ExamFilterParams {
  examTitle?: string;
  subjectId?: number;
  facultyId?: number;
  departmentId?: number;
  status?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface ExamCreatePayload {
  examCode: string;
  examTitle: string;
  description?: string;
  subjectId: number;
  facultyId: number;
  departmentId: number;
  semester: number;
  examType: string;
  totalMarks: number;
  durationMinutes: number;
  passingMarks: number;
  scheduledDate: string; // YYYY-MM-DD
  scheduledEndDate?: string; // YYYY-MM-DD
  startTime: string; // HH:mm:ss
  endTime: string; // HH:mm:ss
  venue?: string;
  instructions?: string;
  status?: string;
}

export const examService = {
  getAll: async (params?: ExamFilterParams) => {
    const res = await api.get('/v1/exams', { params });
    return res.data;
  },

  getById: async (id: string | number) => {
    const res = await api.get(`/v1/exams/${id}`);
    return res.data;
  },

  create: async (payload: ExamCreatePayload) => {
    const res = await api.post('/v1/exams', payload);
    return res.data;
  },

  update: async (id: string | number, payload: ExamCreatePayload) => {
    const res = await api.put(`/v1/exams/${id}`, payload);
    return res.data;
  },

  patch: async (id: string | number, payload: Partial<ExamCreatePayload>) => {
    const res = await api.patch(`/v1/exams/${id}`, payload);
    return res.data;
  },

  delete: async (id: string | number) => {
    const res = await api.delete(`/v1/exams/${id}`);
    return res.data;
  },

  getUpcoming: async () => {
    const res = await api.get('/v1/exams/upcoming');
    return res.data;
  },

  getToday: async () => {
    const res = await api.get('/v1/exams/today');
    return res.data;
  },

  getActive: async () => {
    const res = await api.get('/v1/exams/active');
    return res.data;
  },

  getDashboard: async () => {
    const res = await api.get('/v1/exams/dashboard');
    return res.data;
  },
};
