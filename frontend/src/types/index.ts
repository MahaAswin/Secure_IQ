// User roles
export enum UserRole {
  STUDENT = 'student',
  FACULTY = 'faculty',
  HOD = 'hod',
  ADMIN = 'admin',
}

// Exam status
export enum ExamStatus {
  UPCOMING = 'upcoming',
  LIVE = 'live',
  COMPLETED = 'completed',
  CANCELLED = 'cancelled',
}

// Submission status
export enum SubmissionStatus {
  NOT_STARTED = 'not_started',
  IN_PROGRESS = 'in_progress',
  SUBMITTED = 'submitted',
  FLAGGED = 'flagged',
}

// Alert severity
export enum AlertSeverity {
  LOW = 'low',
  MEDIUM = 'medium',
  HIGH = 'high',
  CRITICAL = 'critical',
}

// User types
export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  avatar?: string;
  department?: string;
  rollNumber?: string;
  employeeId?: string;
  createdAt: string;
  isActive: boolean;
}

export interface Student extends User {
  role: UserRole.STUDENT;
  rollNumber: string;
  semester: number;
  batch: string;
  cgpa: number;
}

export interface Faculty extends User {
  role: UserRole.FACULTY;
  employeeId: string;
  designation: string;
  subjects: string[];
}

export interface HOD extends User {
  role: UserRole.HOD;
  employeeId: string;
  department: string;
}

export interface Admin extends User {
  role: UserRole.ADMIN;
  employeeId: string;
  permissions: string[];
}

// Exam types
export interface Exam {
  id: string;
  title: string;
  subject: string;
  code: string;
  description: string;
  facultyId: string;
  facultyName: string;
  department: string;
  startTime: string;
  endTime: string;
  duration: number; // in minutes
  totalMarks: number;
  passingMarks: number;
  status: ExamStatus;
  venue: string;
  enrolledCount: number;
  maxStudents: number;
  createdAt: string;
  instructions: string[];
  isAIMonitored: boolean;
}

export interface Question {
  id: string;
  examId: string;
  type: 'mcq' | 'short_answer' | 'long_answer' | 'true_false';
  text: string;
  options?: string[];
  correctAnswer?: string;
  marks: number;
  order: number;
}

export interface ExamResult {
  id: string;
  examId: string;
  examTitle: string;
  studentId: string;
  studentName: string;
  obtainedMarks: number;
  totalMarks: number;
  percentage: number;
  grade: string;
  rank?: number;
  submittedAt: string;
  timeTaken: number; // in seconds
  status: SubmissionStatus;
  aiFlags: AIFlag[];
}

// AI Monitoring types
export interface AIFlag {
  id: string;
  type: string;
  severity: AlertSeverity;
  description: string;
  timestamp: string;
  imageUrl?: string;
  confidence: number;
}

export interface MonitoringSession {
  id: string;
  examId: string;
  studentId: string;
  studentName: string;
  startTime: string;
  isActive: boolean;
  flagCount: number;
  lastActivity: string;
  riskScore: number;
  flags: AIFlag[];
}

// Analytics types
export interface AnalyticsMetric {
  label: string;
  value: number;
  change: number;
  changeType: 'increase' | 'decrease';
  period: string;
}

export interface ChartDataPoint {
  name: string;
  value: number;
  [key: string]: string | number;
}

export interface DepartmentStats {
  department: string;
  totalStudents: number;
  totalFaculty: number;
  totalExams: number;
  avgScore: number;
  passRate: number;
}

// Report types
export interface ExamReport {
  id: string;
  examId: string;
  examTitle: string;
  generatedAt: string;
  summary: ReportSummary;
  studentResults: ExamResult[];
  aiInsights: AIInsight[];
  scoreDistribution: ChartDataPoint[];
}

export interface ReportSummary {
  totalStudents: number;
  appeared: number;
  passed: number;
  failed: number;
  avgScore: number;
  highestScore: number;
  lowestScore: number;
  passRate: number;
}

export interface AIInsight {
  id: string;
  category: string;
  insight: string;
  severity: AlertSeverity;
  affectedStudents: number;
  recommendation: string;
}

// Department types
export interface Department {
  id: string;
  name: string;
  code: string;
  hodId?: string;
  hodName?: string;
  facultyCount: number;
  studentCount: number;
  activeExams: number;
  createdAt: string;
}

// Navigation types
export interface NavItem {
  label: string;
  href: string;
  icon: string;
  badge?: number | string;
  children?: NavItem[];
}

// API types
export interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface LoginCredentials {
  email: string;
  password: string;
  role: UserRole;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface AuthState {
  user: User | null;
  tokens: AuthTokens | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
