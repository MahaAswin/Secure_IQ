import { UserRole } from '@/types';

// App Routes
export const ROUTES = {
  ROOT: '/',
  LOGIN: '/login',
  // Student
  STUDENT: {
    DASHBOARD: '/student/dashboard',
    EXAMS: '/student/exams',
    JOIN_EXAM: '/student/exams/:examId/join',
    RESULTS: '/student/results',
    PROFILE: '/student/profile',
  },
  // Faculty
  FACULTY: {
    DASHBOARD: '/faculty/dashboard',
    CREATE_EXAM: '/faculty/exams/create',
    EXAMS: '/faculty/exams',
    EXAM_DETAIL: '/faculty/exams/:examId',
    MONITORING: '/faculty/monitoring',
    STUDENT_MONITORING: '/faculty/monitoring/:examId',
    ANALYTICS: '/faculty/analytics',
    REPORTS: '/faculty/reports',
    REPORT_DETAIL: '/faculty/reports/:examId',
  },
  // HOD
  HOD: {
    DASHBOARD: '/hod/dashboard',
    OVERVIEW: '/hod/overview',
    FACULTY_PERFORMANCE: '/hod/faculty-performance',
    REPORTS: '/hod/reports',
    ANALYTICS: '/hod/analytics',
  },
  // Admin
  ADMIN: {
    DASHBOARD: '/admin/dashboard',
    DEPARTMENTS: '/admin/departments',
    FACULTY: '/admin/faculty',
    STUDENTS: '/admin/students',
    PERMISSIONS: '/admin/permissions',
    SETTINGS: '/admin/settings',
  },
  // Shared
  ANALYTICS: '/analytics',
  REPORTS: '/reports',
  AI_REPORT: '/reports/:examId/ai',
  VENUE_MONITORING: '/monitoring/venue',
  SECURE_BROWSER: '/secure-browser',
  NOT_FOUND: '*',
} as const;

// Default redirects per role
export const ROLE_HOME: Record<UserRole, string> = {
  [UserRole.STUDENT]: ROUTES.STUDENT.DASHBOARD,
  [UserRole.FACULTY]: ROUTES.FACULTY.DASHBOARD,
  [UserRole.HOD]: ROUTES.HOD.DASHBOARD,
  [UserRole.ADMIN]: ROUTES.ADMIN.DASHBOARD,
};

// Role display labels
export const ROLE_LABELS: Record<UserRole, string> = {
  [UserRole.STUDENT]: 'Student',
  [UserRole.FACULTY]: 'Faculty',
  [UserRole.HOD]: 'Head of Department',
  [UserRole.ADMIN]: 'Administrator',
};

// Pagination
export const DEFAULT_PAGE_SIZE = 10;
export const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

// App metadata
export const APP_NAME = 'SecureIQ';
export const APP_TAGLINE = 'Secure Exams. Intelligent Decisions.';
export const APP_DESCRIPTION =
  'AI-powered secure examination platform with real-time monitoring, explainable AI reports, and comprehensive analytics.';

// Local storage keys
export const STORAGE_KEYS = {
  AUTH_TOKEN: 'secureiq_access_token',
  REFRESH_TOKEN: 'secureiq_refresh_token',
  USER: 'secureiq_user',
  THEME: 'secureiq_theme',
} as const;

// Theme
export const THEMES = {
  LIGHT: 'light',
  DARK: 'dark',
} as const;

// Grade thresholds
export const GRADE_THRESHOLDS = [
  { grade: 'O', min: 90 },
  { grade: 'A+', min: 80 },
  { grade: 'A', min: 70 },
  { grade: 'B+', min: 60 },
  { grade: 'B', min: 50 },
  { grade: 'C', min: 40 },
  { grade: 'F', min: 0 },
];

// AI monitoring
export const AI_FLAG_TYPES = [
  'Face Not Detected',
  'Multiple Faces',
  'Looking Away',
  'Phone Detected',
  'Unusual Activity',
  'Tab Switch',
  'Copy Paste',
  'Screen Sharing',
] as const;

export const RISK_LEVELS = {
  LOW: { label: 'Low Risk', color: 'success', max: 30 },
  MEDIUM: { label: 'Medium Risk', color: 'warning', max: 60 },
  HIGH: { label: 'High Risk', color: 'danger', max: 100 },
} as const;

// Chart colors
export const CHART_COLORS = [
  '#2563EB',
  '#7C3AED',
  '#16A34A',
  '#F59E0B',
  '#DC2626',
  '#0891B2',
  '#DB2777',
  '#D97706',
] as const;
