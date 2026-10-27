import axios, { AxiosError } from 'axios';
import { STORAGE_KEYS } from '@/constants';

// Resolve API base URL: prefer VITE_API_BASE_URL, fallback to VITE_API_URL or localhost:8080
const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '');
const computedBaseUrl = rawBaseUrl.endsWith('/api') ? rawBaseUrl : `${rawBaseUrl}/api`;

const api = axios.create({
  baseURL: computedBaseUrl,
  timeout: 20000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor — attach access token and normalize path prefixes
api.interceptors.request.use(
  (config) => {
    // If request starts with /api/v1, strip redundant leading /api
    if (config.url?.startsWith('/api/v1')) {
      config.url = config.url.replace(/^\/api/, '');
    }
    const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — handle 401 (unauthorized) and 403 (forbidden)
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
      localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
      localStorage.removeItem(STORAGE_KEYS.USER);
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
