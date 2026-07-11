import { User, Student, Faculty, ExamResult, MonitoringSession, Exam, Department, AIInsight } from '@/types';
import {
  UserRole,
  ExamStatus,
  SubmissionStatus,
  AlertSeverity,
} from '@/types';

// ─── Mock Users ───────────────────────────────────────────────────────────────

export const mockStudents: Student[] = [
  {
    id: 'stu-001',
    name: 'Arjun Sharma',
    email: 'arjun.sharma@secureiq.edu',
    role: UserRole.STUDENT,
    rollNumber: 'CS2021001',
    semester: 6,
    batch: '2021-2025',
    department: 'Computer Science',
    cgpa: 8.7,
    isActive: true,
    createdAt: '2021-08-01T00:00:00Z',
  },
  {
    id: 'stu-002',
    name: 'Priya Nair',
    email: 'priya.nair@secureiq.edu',
    role: UserRole.STUDENT,
    rollNumber: 'CS2021002',
    semester: 6,
    batch: '2021-2025',
    department: 'Computer Science',
    cgpa: 9.1,
    isActive: true,
    createdAt: '2021-08-01T00:00:00Z',
  },
  {
    id: 'stu-003',
    name: 'Rohit Verma',
    email: 'rohit.verma@secureiq.edu',
    role: UserRole.STUDENT,
    rollNumber: 'EC2021015',
    semester: 6,
    batch: '2021-2025',
    department: 'Electronics',
    cgpa: 7.4,
    isActive: true,
    createdAt: '2021-08-01T00:00:00Z',
  },
  {
    id: 'stu-004',
    name: 'Sneha Patel',
    email: 'sneha.patel@secureiq.edu',
    role: UserRole.STUDENT,
    rollNumber: 'ME2021020',
    semester: 6,
    batch: '2021-2025',
    department: 'Mechanical',
    cgpa: 8.2,
    isActive: false,
    createdAt: '2021-08-01T00:00:00Z',
  },
  {
    id: 'stu-005',
    name: 'Karthik Reddy',
    email: 'karthik.reddy@secureiq.edu',
    role: UserRole.STUDENT,
    rollNumber: 'CS2021005',
    semester: 6,
    batch: '2021-2025',
    department: 'Computer Science',
    cgpa: 6.8,
    isActive: true,
    createdAt: '2021-08-01T00:00:00Z',
  },
];

export const mockFaculty: Faculty[] = [
  {
    id: 'fac-001',
    name: 'Dr. Meera Krishnan',
    email: 'meera.krishnan@secureiq.edu',
    role: UserRole.FACULTY,
    employeeId: 'EMP001',
    designation: 'Associate Professor',
    department: 'Computer Science',
    subjects: ['Data Structures', 'Algorithms', 'Database Systems'],
    isActive: true,
    createdAt: '2018-07-01T00:00:00Z',
  },
  {
    id: 'fac-002',
    name: 'Prof. Rajesh Iyer',
    email: 'rajesh.iyer@secureiq.edu',
    role: UserRole.FACULTY,
    employeeId: 'EMP002',
    designation: 'Professor',
    department: 'Computer Science',
    subjects: ['Machine Learning', 'AI', 'Pattern Recognition'],
    isActive: true,
    createdAt: '2015-07-01T00:00:00Z',
  },
];

export const mockCurrentStudent: Student = mockStudents[0];
export const mockCurrentFaculty: Faculty = mockFaculty[0];
export const mockCurrentHOD: User = {
  id: 'hod-001',
  name: 'Dr. Suresh Babu',
  email: 'suresh.babu@secureiq.edu',
  role: UserRole.HOD,
  department: 'Computer Science',
  isActive: true,
  createdAt: '2010-07-01T00:00:00Z',
};
export const mockCurrentAdmin: User = {
  id: 'adm-001',
  name: 'Admin Kumar',
  email: 'admin@secureiq.edu',
  role: UserRole.ADMIN,
  isActive: true,
  createdAt: '2020-01-01T00:00:00Z',
};

// ─── Mock Exams ───────────────────────────────────────────────────────────────

export const mockExams: Exam[] = [
  {
    id: 'exam-001',
    title: 'Data Structures & Algorithms',
    subject: 'CS301',
    code: 'CS301-MID-2024',
    description: 'Mid-semester examination covering arrays, linked lists, trees, and graphs.',
    facultyId: 'fac-001',
    facultyName: 'Dr. Meera Krishnan',
    department: 'Computer Science',
    startTime: new Date(Date.now() + 3600000).toISOString(),
    endTime: new Date(Date.now() + 10800000).toISOString(),
    duration: 120,
    totalMarks: 100,
    passingMarks: 40,
    status: ExamStatus.UPCOMING,
    venue: 'Hall A - Room 101',
    enrolledCount: 45,
    maxStudents: 50,
    createdAt: '2024-01-10T00:00:00Z',
    instructions: [
      'No electronic devices allowed.',
      'Rough work must be done on the provided sheets.',
      'All questions are compulsory.',
    ],
    isAIMonitored: true,
  },
  {
    id: 'exam-002',
    title: 'Machine Learning Fundamentals',
    subject: 'CS401',
    code: 'CS401-FINAL-2024',
    description: 'Final examination on supervised and unsupervised learning algorithms.',
    facultyId: 'fac-002',
    facultyName: 'Prof. Rajesh Iyer',
    department: 'Computer Science',
    startTime: new Date(Date.now() + 86400000).toISOString(),
    endTime: new Date(Date.now() + 97200000).toISOString(),
    duration: 180,
    totalMarks: 100,
    passingMarks: 40,
    status: ExamStatus.UPCOMING,
    venue: 'Hall B - Room 204',
    enrolledCount: 38,
    maxStudents: 40,
    createdAt: '2024-01-08T00:00:00Z',
    instructions: [
      'Calculators are permitted.',
      'Formula sheets will be provided.',
    ],
    isAIMonitored: true,
  },
  {
    id: 'exam-003',
    title: 'Database Management Systems',
    subject: 'CS302',
    code: 'CS302-MID-2024',
    description: 'Mid-semester covering ER diagrams, normalization, and SQL.',
    facultyId: 'fac-001',
    facultyName: 'Dr. Meera Krishnan',
    department: 'Computer Science',
    startTime: new Date(Date.now() - 86400000).toISOString(),
    endTime: new Date(Date.now() - 79200000).toISOString(),
    duration: 120,
    totalMarks: 100,
    passingMarks: 40,
    status: ExamStatus.COMPLETED,
    venue: 'Hall A - Room 103',
    enrolledCount: 48,
    maxStudents: 50,
    createdAt: '2024-01-05T00:00:00Z',
    instructions: ['No devices allowed.'],
    isAIMonitored: true,
  },
];

// ─── Mock Results ─────────────────────────────────────────────────────────────

export const mockResults: ExamResult[] = [
  {
    id: 'res-001',
    examId: 'exam-003',
    examTitle: 'Database Management Systems',
    studentId: 'stu-001',
    studentName: 'Arjun Sharma',
    obtainedMarks: 82,
    totalMarks: 100,
    percentage: 82,
    grade: 'A+',
    rank: 3,
    submittedAt: new Date(Date.now() - 75600000).toISOString(),
    timeTaken: 6840,
    status: SubmissionStatus.SUBMITTED,
    aiFlags: [],
  },
  {
    id: 'res-002',
    examId: 'exam-003',
    examTitle: 'Database Management Systems',
    studentId: 'stu-002',
    studentName: 'Priya Nair',
    obtainedMarks: 91,
    totalMarks: 100,
    percentage: 91,
    grade: 'O',
    rank: 1,
    submittedAt: new Date(Date.now() - 75000000).toISOString(),
    timeTaken: 6200,
    status: SubmissionStatus.SUBMITTED,
    aiFlags: [],
  },
];

// ─── Mock Monitoring Sessions ────────────────────────────────────────────────

export const mockMonitoringSessions: MonitoringSession[] = [
  {
    id: 'mon-001',
    examId: 'exam-001',
    studentId: 'stu-001',
    studentName: 'Arjun Sharma',
    startTime: new Date(Date.now() - 1800000).toISOString(),
    isActive: true,
    flagCount: 1,
    lastActivity: new Date(Date.now() - 60000).toISOString(),
    riskScore: 15,
    flags: [
      {
        id: 'flag-001',
        type: 'Looking Away',
        severity: AlertSeverity.LOW,
        description: 'Student briefly looked away from screen.',
        timestamp: new Date(Date.now() - 900000).toISOString(),
        confidence: 0.78,
      },
    ],
  },
  {
    id: 'mon-002',
    examId: 'exam-001',
    studentId: 'stu-002',
    studentName: 'Priya Nair',
    startTime: new Date(Date.now() - 1800000).toISOString(),
    isActive: true,
    flagCount: 3,
    lastActivity: new Date(Date.now() - 30000).toISOString(),
    riskScore: 55,
    flags: [
      {
        id: 'flag-002',
        type: 'Multiple Faces',
        severity: AlertSeverity.HIGH,
        description: 'Two faces detected in frame.',
        timestamp: new Date(Date.now() - 1200000).toISOString(),
        confidence: 0.92,
      },
      {
        id: 'flag-003',
        type: 'Phone Detected',
        severity: AlertSeverity.CRITICAL,
        description: 'Mobile phone detected in camera view.',
        timestamp: new Date(Date.now() - 600000).toISOString(),
        confidence: 0.95,
      },
    ],
  },
  {
    id: 'mon-003',
    examId: 'exam-001',
    studentId: 'stu-003',
    studentName: 'Rohit Verma',
    startTime: new Date(Date.now() - 1800000).toISOString(),
    isActive: true,
    flagCount: 0,
    lastActivity: new Date(Date.now() - 10000).toISOString(),
    riskScore: 5,
    flags: [],
  },
];

// ─── Mock Departments ─────────────────────────────────────────────────────────

export const mockDepartments: Department[] = [
  { id: 'dept-001', name: 'Computer Science', code: 'CS', hodId: 'hod-001', hodName: 'Dr. Suresh Babu', facultyCount: 12, studentCount: 320, activeExams: 3, createdAt: '2010-07-01T00:00:00Z' },
  { id: 'dept-002', name: 'Electronics & Communication', code: 'EC', hodId: 'hod-002', hodName: 'Dr. Anitha Rao', facultyCount: 10, studentCount: 280, activeExams: 2, createdAt: '2010-07-01T00:00:00Z' },
  { id: 'dept-003', name: 'Mechanical Engineering', code: 'ME', hodId: 'hod-003', hodName: 'Dr. Vikram Singh', facultyCount: 9, studentCount: 250, activeExams: 1, createdAt: '2010-07-01T00:00:00Z' },
  { id: 'dept-004', name: 'Civil Engineering', code: 'CE', hodId: 'hod-004', hodName: 'Prof. Lakshmi Devi', facultyCount: 8, studentCount: 200, activeExams: 2, createdAt: '2010-07-01T00:00:00Z' },
];

// ─── Mock Chart Data ──────────────────────────────────────────────────────────

export const mockScoreDistribution = [
  { name: '0-40', value: 5 },
  { name: '40-50', value: 8 },
  { name: '50-60', value: 12 },
  { name: '60-70', value: 18 },
  { name: '70-80', value: 22 },
  { name: '80-90', value: 15 },
  { name: '90-100', value: 10 },
];

export const mockExamTrend = [
  { name: 'Jan', exams: 4, students: 120, avgScore: 72 },
  { name: 'Feb', exams: 6, students: 180, avgScore: 74 },
  { name: 'Mar', exams: 8, students: 240, avgScore: 71 },
  { name: 'Apr', exams: 5, students: 150, avgScore: 76 },
  { name: 'May', exams: 10, students: 300, avgScore: 78 },
  { name: 'Jun', exams: 7, students: 210, avgScore: 75 },
];

export const mockDeptPerformance = [
  { name: 'CS', avgScore: 78, passRate: 88 },
  { name: 'EC', avgScore: 74, passRate: 82 },
  { name: 'ME', avgScore: 70, passRate: 79 },
  { name: 'CE', avgScore: 72, passRate: 80 },
];

export const mockAIInsights: AIInsight[] = [
  {
    id: 'insight-001',
    category: 'Behavioral Pattern',
    insight: '12% of students showed increased gaze deviation in the final 30 minutes.',
    severity: AlertSeverity.MEDIUM,
    affectedStudents: 5,
    recommendation: 'Consider reviewing exam timing and length.',
  },
  {
    id: 'insight-002',
    category: 'Device Detection',
    insight: '3 students had unauthorized devices flagged during the exam.',
    severity: AlertSeverity.HIGH,
    affectedStudents: 3,
    recommendation: 'Strict verification required before next exam session.',
  },
  {
    id: 'insight-003',
    category: 'Performance Anomaly',
    insight: 'Students scoring below 50 had an average of 4.2 AI flags vs 0.8 for those above 75.',
    severity: AlertSeverity.LOW,
    affectedStudents: 8,
    recommendation: 'No direct action needed; correlation noted for future analysis.',
  },
];
