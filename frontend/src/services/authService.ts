import api from './api';
import { User, LoginCredentials } from '@/types';

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: string;
  phoneNumber?: string;
}

export const authService = {
  login: async (credentials: { email: string; password: string }) => {
    const res = await api.post('/v1/auth/login', credentials);
    return res.data;
  },

  register: async (payload: RegisterPayload) => {
    const res = await api.post('/v1/auth/register', payload);
    return res.data;
  },

  getMe: async () => {
    const res = await api.get('/v1/auth/me');
    return res.data;
  },

  logout: async () => {
    try {
      await api.post('/v1/auth/logout');
    } catch {
      // Ignore network errors on logout
    }
  },
};
