import api from './api';

export interface DepartmentFilterParams {
  departmentName?: string;
  departmentCode?: string;
  hodId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export const departmentService = {
  getAll: async (params?: DepartmentFilterParams) => {
    const res = await api.get('/v1/departments', { params });
    return res.data;
  },

  getById: async (id: string | number) => {
    const res = await api.get(`/v1/departments/${id}`);
    return res.data;
  },

  getStats: async () => {
    const res = await api.get('/v1/departments/stats');
    return res.data;
  },

  create: async (payload: any) => {
    const res = await api.post('/v1/departments', payload);
    return res.data;
  },

  update: async (id: string | number, payload: any) => {
    const res = await api.put(`/v1/departments/${id}`, payload);
    return res.data;
  },

  patch: async (id: string | number, payload: any) => {
    const res = await api.patch(`/v1/departments/${id}`, payload);
    return res.data;
  },

  delete: async (id: string | number) => {
    const res = await api.delete(`/v1/departments/${id}`);
    return res.data;
  },
};
