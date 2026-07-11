import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { User, AuthState, LoginCredentials, UserRole } from '@/types';
import { STORAGE_KEYS, ROLE_HOME } from '@/constants';
import { safeJsonParse } from '@/utils';
import api from '@/services/api';

export interface RegisterData {
  name: string;
  email: string;
  password: string;
  role: UserRole;
}

interface AuthContextValue extends AuthState {
  login: (credentials: LoginCredentials) => Promise<void>;
  registerUser: (data: RegisterData) => Promise<void>;
  logout: () => void;
  updateUser: (user: Partial<User>) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function normalizeUser(user: any): User | null {
  if (!user) return null;

  let role = user.role;
  if (typeof role === 'string') {
    role = role.toLowerCase() as UserRole;
  }

  let name = user.name;
  if (!name && (user.firstName || user.lastName)) {
    name = `${user.firstName || ''} ${user.lastName || ''}`.trim();
  }
  if (!name) {
    name = user.email ? user.email.split('@')[0] : 'User';
  }

  const isActive = user.isActive !== undefined ? user.isActive : (user.status === 'ACTIVE');

  return {
    ...user,
    name,
    role,
    isActive,
  };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({
    user: normalizeUser(safeJsonParse<any>(localStorage.getItem(STORAGE_KEYS.USER), null)),
    tokens: null,
    isAuthenticated: !!localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN),
    isLoading: false,
  });

  // Sync from storage on mount
  useEffect(() => {
    const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
    const userRaw = safeJsonParse<any>(localStorage.getItem(STORAGE_KEYS.USER), null);
    if (token && userRaw) {
      const user = normalizeUser(userRaw);
      setState((prev) => ({ ...prev, user, isAuthenticated: true }));
    }
  }, []);

  const login = useCallback(async (credentials: LoginCredentials) => {
    setState((prev) => ({ ...prev, isLoading: true }));
    try {
      const response = await api.post('/v1/auth/login', {
        email: credentials.email,
        password: credentials.password,
      });
      const { token, user: userRaw } = response.data.data;
      const user = normalizeUser(userRaw);

      localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, token);
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));

      setState({
        user,
        tokens: { accessToken: token, refreshToken: token, expiresIn: 86400 },
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (error) {
      setState((prev) => ({ ...prev, isLoading: false }));
      throw error;
    }
  }, []);

  const registerUser = useCallback(async (data: RegisterData) => {
    setState((prev) => ({ ...prev, isLoading: true }));
    try {
      const response = await api.post('/v1/auth/register', data);
      const { token, user: userRaw } = response.data.data;
      const user = normalizeUser(userRaw);

      localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, token);
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));

      setState({
        user,
        tokens: { accessToken: token, refreshToken: token, expiresIn: 86400 },
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (error) {
      setState((prev) => ({ ...prev, isLoading: false }));
      throw error;
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
    localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
    localStorage.removeItem(STORAGE_KEYS.USER);
    setState({ user: null, tokens: null, isAuthenticated: false, isLoading: false });
  }, []);

  const updateUser = useCallback((updates: Partial<User>) => {
    setState((prev) => {
      if (!prev.user) return prev;
      const updated = normalizeUser({ ...prev.user, ...updates });
      if (updated) {
        localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(updated));
        return { ...prev, user: updated };
      }
      return prev;
    });
  }, []);

  return (
    <AuthContext.Provider value={{ ...state, login, registerUser, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

export function useRoleHome(): string {
  const { user } = useAuth();
  if (!user) return '/login';
  return ROLE_HOME[user.role];
}
