import api from './api';

export interface SubjectFilterParams {
  subjectName?: string;
  subjectCode?: string;
  departmentId?: number;
  semester?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export const subjectService = {
  getAll: async (params?: SubjectFilterParams) => {
    const res = await api.get('/v1/subjects', { params });
    return res.data;
  },

  getById: async (id: string | number) => {
    const res = await api.get(`/v1/subjects/${id}`);
    return res.data;
  },

  create: async (payload: any) => {
    const res = await api.post('/v1/subjects', payload);
    return res.data;
  },

  update: async (id: string | number, payload: any) => {
    const res = await api.put(`/v1/subjects/${id}`, payload);
    return res.data;
  },

  patch: async (id: string | number, payload: any) => {
    const res = await api.patch(`/v1/subjects/${id}`, payload);
    return res.data;
  },

  delete: async (id: string | number) => {
    const res = await api.delete(`/v1/subjects/${id}`);
    return res.data;
  },
};
