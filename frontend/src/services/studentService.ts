import api from './api';

export interface StudentFilterParams {
  name?: string;
  registerNumber?: string;
  rollNumber?: string;
  departmentId?: number;
  academicYear?: string;
  semester?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export const studentService = {
  getAll: async (params?: StudentFilterParams) => {
    const res = await api.get('/v1/students', { params });
    return res.data;
  },

  getById: async (id: string | number) => {
    const res = await api.get(`/v1/students/${id}`);
    return res.data;
  },

  getProfile: async () => {
    const res = await api.get('/v1/students/profile');
    return res.data;
  },

  getDashboard: async () => {
    const res = await api.get('/v1/students/dashboard');
    return res.data;
  },

  getDashboardExams: async () => {
    const res = await api.get('/v1/students/dashboard/exams');
    return res.data;
  },

  getDashboardActivities: async () => {
    const res = await api.get('/v1/students/dashboard/activities');
    return res.data;
  },

  getDashboardNotifications: async () => {
    const res = await api.get('/v1/students/dashboard/notifications');
    return res.data;
  },

  create: async (payload: any) => {
    const res = await api.post('/v1/students', payload);
    return res.data;
  },

  update: async (id: string | number, payload: any) => {
    const res = await api.put(`/v1/students/${id}`, payload);
    return res.data;
  },

  patch: async (id: string | number, payload: any) => {
    const res = await api.patch(`/v1/students/${id}`, payload);
    return res.data;
  },

  delete: async (id: string | number) => {
    const res = await api.delete(`/v1/students/${id}`);
    return res.data;
  },
};
