import api from './api';

export interface FacultyFilterParams {
  name?: string;
  employeeId?: string;
  departmentId?: number;
  specialization?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export const facultyService = {
  getAll: async (params?: FacultyFilterParams) => {
    const res = await api.get('/v1/faculties', { params });
    return res.data;
  },

  getById: async (id: string | number) => {
    const res = await api.get(`/v1/faculties/${id}`);
    return res.data;
  },

  getProfile: async () => {
    const res = await api.get('/v1/faculties/profile');
    return res.data;
  },

  getDashboard: async () => {
    const res = await api.get('/v1/faculties/dashboard');
    return res.data;
  },

  getAssignedSubjects: async () => {
    const res = await api.get('/v1/faculties/dashboard/subjects');
    return res.data;
  },

  getAssignedStudents: async () => {
    const res = await api.get('/v1/faculties/dashboard/students');
    return res.data;
  },

  getUpcomingExams: async () => {
    const res = await api.get('/v1/faculties/dashboard/exams');
    return res.data;
  },

  getRecentActivities: async () => {
    const res = await api.get('/v1/faculties/dashboard/activities');
    return res.data;
  },

  create: async (payload: any) => {
    const res = await api.post('/v1/faculties', payload);
    return res.data;
  },

  update: async (id: string | number, payload: any) => {
    const res = await api.put(`/v1/faculties/${id}`, payload);
    return res.data;
  },

  patch: async (id: string | number, payload: any) => {
    const res = await api.patch(`/v1/faculties/${id}`, payload);
    return res.data;
  },

  delete: async (id: string | number) => {
    const res = await api.delete(`/v1/faculties/${id}`);
    return res.data;
  },
};
