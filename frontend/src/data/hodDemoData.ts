/**
 * SecureIQ — HOD Demonstration Analytics Dataset
 * Dedicated enterprise demo-data module providing mathematically consistent,
 * interconnected department analytics for the Head of Department dashboard.
 */

export interface DemoFaculty {
  id: string;
  employeeId: string;
  name: string;
  firstName: string;
  lastName: string;
  designation: string;
  department: string;
  subjects: { id: string; subjectName: string; subjectCode: string }[];
  examsConducted: number;
  studentsEvaluated: number;
  averageScore: number;
  passRate: number;
  status: 'ACTIVE' | 'ON_LEAVE';
  workloadCategory: 'Balanced' | 'High' | 'Low';
  examsAssigned: number;
  studentsAssigned: number;
  user: {
    firstName: string;
    lastName: string;
    email: string;
    status: 'ACTIVE' | 'INACTIVE';
  };
}

export interface DepartmentSummaryKPIs {
  totalFaculty: number;
  activeFaculty: number;
  onLeaveFaculty: number;
  activeExams: number;
  liveExams: number;
  scheduledExams: number;
  averageScore: number;
  scoreSemesterChange: number;
  passRate: number;
  passRateSemesterChange: number;
  totalEvaluatedStudents: number;
}

export interface ScoreDistributionItem {
  name: string;
  shortName: string;
  percentage: number;
  value: number; // student count
  color: string;
  range: string;
}

export interface ExamTrendPoint {
  name: string;
  exams: number;
  avgScore: number;
  passRate: number;
  students: number;
}

export interface SubjectPerformance {
  id: string;
  subjectName: string;
  subjectCode: string;
  studentsEvaluated: number;
  averageScore: number;
  passRate: number;
  facultyInCharge: string;
}

export interface RecentExamRecord {
  id: string;
  examCode: string;
  title: string;
  subject: string;
  faculty: string;
  date: string;
  studentsEnrolled: number;
  studentsEvaluated: number;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'RESULTS_PUBLISHED';
}

export interface FacultyWorkloadOverview {
  facultyName: string;
  employeeId: string;
  examsAssigned: number;
  studentsAssigned: number;
  workloadCategory: 'Balanced' | 'High' | 'Low';
  capacityPercentage: number;
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. KPI Summaries
// ─────────────────────────────────────────────────────────────────────────────

export const HOD_KPI_SUMMARY: DepartmentSummaryKPIs = {
  totalFaculty: 24,
  activeFaculty: 21,
  onLeaveFaculty: 3,
  activeExams: 6,
  liveExams: 4,
  scheduledExams: 2,
  averageScore: 76.2,
  scoreSemesterChange: 2.1,
  passRate: 88.0,
  passRateSemesterChange: 3.2,
  totalEvaluatedStudents: 1200,
};

// ─────────────────────────────────────────────────────────────────────────────
// 2. Realistic Faculty Members (8 Comprehensive Profiles)
// ─────────────────────────────────────────────────────────────────────────────

export const HOD_DEMO_FACULTY: DemoFaculty[] = [
  {
    id: 'fac-01',
    employeeId: 'FAC-CS-0101',
    name: 'Meera Krishnan',
    firstName: 'Meera',
    lastName: 'Krishnan',
    designation: 'Associate Professor',
    department: 'Computer Science & Engineering',
    subjects: [
      { id: 'sub-01', subjectName: 'Algorithms', subjectCode: 'CS301' },
      { id: 'sub-02', subjectName: 'Data Structures', subjectCode: 'CS201' },
    ],
    examsConducted: 8,
    studentsEvaluated: 210,
    averageScore: 81.4,
    passRate: 92.5,
    status: 'ACTIVE',
    workloadCategory: 'High',
    examsAssigned: 8,
    studentsAssigned: 210,
    user: {
      firstName: 'Meera',
      lastName: 'Krishnan',
      email: 'meera.krishnan@secureiq.edu',
      status: 'ACTIVE',
    },
  },
  {
    id: 'fac-02',
    employeeId: 'FAC-CS-0102',
    name: 'Arun Kumar',
    firstName: 'Arun',
    lastName: 'Kumar',
    designation: 'Assistant Professor',
    department: 'Computer Science & Engineering',
    subjects: [
      { id: 'sub-03', subjectName: 'Database Systems', subjectCode: 'CS302' },
    ],
    examsConducted: 6,
    studentsEvaluated: 165,
    averageScore: 74.8,
    passRate: 86.0,
    status: 'ACTIVE',
    workloadCategory: 'Balanced',
    examsAssigned: 6,
    studentsAssigned: 165,
    user: {
      firstName: 'Arun',
      lastName: 'Kumar',
      email: 'arun.kumar@secureiq.edu',
      status: 'ACTIVE',
    },
  },
  {
    id: 'fac-03',
    employeeId: 'FAC-CS-0103',
    name: 'Priya Raman',
    firstName: 'Priya',
    lastName: 'Raman',
    designation: 'Associate Professor',
    department: 'Computer Science & Engineering',
    subjects: [
      { id: 'sub-04', subjectName: 'Operating Systems', subjectCode: 'CS303' },
    ],
    examsConducted: 7,
    studentsEvaluated: 180,
    averageScore: 79.2,
    passRate: 91.0,
    status: 'ACTIVE',
    workloadCategory: 'High',
    examsAssigned: 7,
    studentsAssigned: 180,
    user: {
      firstName: 'Priya',
      lastName: 'Raman',
      email: 'priya.raman@secureiq.edu',
      status: 'ACTIVE',
    },
  },
  {
    id: 'fac-04',
    employeeId: 'FAC-CS-0104',
    name: 'Karthik Raj',
    firstName: 'Karthik',
    lastName: 'Raj',
    designation: 'Assistant Professor',
    department: 'Computer Science & Engineering',
    subjects: [
      { id: 'sub-05', subjectName: 'Computer Networks', subjectCode: 'CS304' },
    ],
    examsConducted: 5,
    studentsEvaluated: 140,
    averageScore: 73.5,
    passRate: 84.5,
    status: 'ACTIVE',
    workloadCategory: 'Balanced',
    examsAssigned: 5,
    studentsAssigned: 140,
    user: {
      firstName: 'Karthik',
      lastName: 'Raj',
      email: 'karthik.raj@secureiq.edu',
      status: 'ACTIVE',
    },
  },
  {
    id: 'fac-05',
    employeeId: 'FAC-CS-0105',
    name: 'Divya Shankar',
    firstName: 'Divya',
    lastName: 'Shankar',
    designation: 'Assistant Professor',
    department: 'Computer Science & Engineering',
    subjects: [
      { id: 'sub-06', subjectName: 'Software Engineering', subjectCode: 'CS305' },
    ],
    examsConducted: 6,
    studentsEvaluated: 155,
    averageScore: 77.0,
    passRate: 89.0,
    status: 'ACTIVE',
    workloadCategory: 'Balanced',
    examsAssigned: 6,
    studentsAssigned: 155,
    user: {
      firstName: 'Divya',
      lastName: 'Shankar',
      email: 'divya.shankar@secureiq.edu',
      status: 'ACTIVE',
    },
  },
  {
    id: 'fac-06',
    employeeId: 'FAC-CS-0106',
    name: 'Naveen Prakash',
    firstName: 'Naveen',
    lastName: 'Prakash',
    designation: 'Professor',
    department: 'Computer Science & Engineering',
    subjects: [
      { id: 'sub-07', subjectName: 'Artificial Intelligence', subjectCode: 'CS401' },
    ],
    examsConducted: 5,
    studentsEvaluated: 125,
    averageScore: 83.6,
    passRate: 94.0,
    status: 'ACTIVE',
    workloadCategory: 'Balanced',
    examsAssigned: 5,
    studentsAssigned: 125,
    user: {
      firstName: 'Naveen',
      lastName: 'Prakash',
      email: 'naveen.prakash@secureiq.edu',
      status: 'ACTIVE',
    },
  },
  {
    id: 'fac-07',
    employeeId: 'FAC-CS-0107',
    name: 'Anitha Devi',
    firstName: 'Anitha',
    lastName: 'Devi',
    designation: 'Assistant Professor',
    department: 'Computer Science & Engineering',
    subjects: [
      { id: 'sub-08', subjectName: 'Cyber Security', subjectCode: 'CS402' },
    ],
    examsConducted: 4,
    studentsEvaluated: 95,
    averageScore: 72.0,
    passRate: 82.0,
    status: 'ON_LEAVE',
    workloadCategory: 'Low',
    examsAssigned: 4,
    studentsAssigned: 95,
    user: {
      firstName: 'Anitha',
      lastName: 'Devi',
      email: 'anitha.devi@secureiq.edu',
      status: 'INACTIVE',
    },
  },
  {
    id: 'fac-08',
    employeeId: 'FAC-CS-0108',
    name: 'Rahul Varma',
    firstName: 'Rahul',
    lastName: 'Varma',
    designation: 'Associate Professor',
    department: 'Computer Science & Engineering',
    subjects: [
      { id: 'sub-09', subjectName: 'Machine Learning', subjectCode: 'CS403' },
    ],
    examsConducted: 5,
    studentsEvaluated: 130,
    averageScore: 80.5,
    passRate: 90.0,
    status: 'ACTIVE',
    workloadCategory: 'Balanced',
    examsAssigned: 5,
    studentsAssigned: 130,
    user: {
      firstName: 'Rahul',
      lastName: 'Varma',
      email: 'rahul.varma@secureiq.edu',
      status: 'ACTIVE',
    },
  },
];

// ─────────────────────────────────────────────────────────────────────────────
// 3. Score Distribution (Mathematically sums to exactly 1,200 students = 100%)
// ─────────────────────────────────────────────────────────────────────────────

export const HOD_SCORE_DISTRIBUTION: ScoreDistributionItem[] = [
  {
    name: 'Excellent (85-100%)',
    shortName: 'Excellent',
    percentage: 25,
    value: 300,
    color: '#10B981', // Emerald green
    range: '85 - 100%',
  },
  {
    name: 'Good (70-84%)',
    shortName: 'Good',
    percentage: 35,
    value: 420,
    color: '#3B82F6', // Blue
    range: '70 - 84%',
  },
  {
    name: 'Average (50-69%)',
    shortName: 'Average',
    percentage: 28,
    value: 336,
    color: '#F59E0B', // Amber
    range: '50 - 69%',
  },
  {
    name: 'Needs Improvement (<50%)',
    shortName: 'Needs Improvement',
    percentage: 12,
    value: 144,
    color: '#EF4444', // Red
    range: 'Below 50%',
  },
];

// ─────────────────────────────────────────────────────────────────────────────
// 4. Exam Trends (6 Months of Historical & Current Progression)
// ─────────────────────────────────────────────────────────────────────────────

export const HOD_EXAM_TRENDS: ExamTrendPoint[] = [
  { name: 'Nov', exams: 4, avgScore: 73.5, passRate: 85.0, students: 680 },
  { name: 'Dec', exams: 6, avgScore: 74.2, passRate: 86.5, students: 920 },
  { name: 'Jan', exams: 3, avgScore: 75.0, passRate: 87.0, students: 510 },
  { name: 'Feb', exams: 5, avgScore: 74.8, passRate: 86.8, students: 790 },
  { name: 'Mar', exams: 8, avgScore: 75.8, passRate: 87.5, students: 1140 },
  { name: 'Apr', exams: 6, avgScore: 76.2, passRate: 88.0, students: 1200 },
];

// ─────────────────────────────────────────────────────────────────────────────
// 5. Subject-Wise Performance (8 Curriculum Subjects)
// ─────────────────────────────────────────────────────────────────────────────

export const HOD_SUBJECT_PERFORMANCE: SubjectPerformance[] = [
  {
    id: 'sub-01',
    subjectName: 'Artificial Intelligence',
    subjectCode: 'CS401',
    studentsEvaluated: 125,
    averageScore: 83.6,
    passRate: 94.0,
    facultyInCharge: 'Prof. Naveen Prakash',
  },
  {
    id: 'sub-02',
    subjectName: 'Algorithms',
    subjectCode: 'CS301',
    studentsEvaluated: 180,
    averageScore: 81.2,
    passRate: 92.0,
    facultyInCharge: 'Dr. Meera Krishnan',
  },
  {
    id: 'sub-03',
    subjectName: 'Machine Learning',
    subjectCode: 'CS403',
    studentsEvaluated: 120,
    averageScore: 80.5,
    passRate: 90.0,
    facultyInCharge: 'Dr. Rahul Varma',
  },
  {
    id: 'sub-04',
    subjectName: 'Operating Systems',
    subjectCode: 'CS303',
    studentsEvaluated: 180,
    averageScore: 79.2,
    passRate: 91.0,
    facultyInCharge: 'Dr. Priya Raman',
  },
  {
    id: 'sub-05',
    subjectName: 'Data Structures',
    subjectCode: 'CS201',
    studentsEvaluated: 195,
    averageScore: 78.4,
    passRate: 90.2,
    facultyInCharge: 'Dr. Meera Krishnan',
  },
  {
    id: 'sub-06',
    subjectName: 'Software Engineering',
    subjectCode: 'CS305',
    studentsEvaluated: 155,
    averageScore: 77.0,
    passRate: 89.0,
    facultyInCharge: 'Prof. Divya Shankar',
  },
  {
    id: 'sub-07',
    subjectName: 'Database Systems',
    subjectCode: 'CS302',
    studentsEvaluated: 165,
    averageScore: 74.8,
    passRate: 86.0,
    facultyInCharge: 'Prof. Arun Kumar',
  },
  {
    id: 'sub-08',
    subjectName: 'Computer Networks',
    subjectCode: 'CS304',
    studentsEvaluated: 140,
    averageScore: 73.5,
    passRate: 84.5,
    facultyInCharge: 'Prof. Karthik Raj',
  },
  {
    id: 'sub-09',
    subjectName: 'Cyber Security',
    subjectCode: 'CS402',
    studentsEvaluated: 95,
    averageScore: 72.0,
    passRate: 82.0,
    facultyInCharge: 'Prof. Anitha Devi',
  },
];

// ─────────────────────────────────────────────────────────────────────────────
// 6. Recent Examination Activity (6 Cohesive Records)
// ─────────────────────────────────────────────────────────────────────────────

export const HOD_RECENT_EXAMS: RecentExamRecord[] = [
  {
    id: 'exam-rec-01',
    examCode: 'EX-CS-301-M1',
    title: 'Mid-Term Algorithms & Complexity',
    subject: 'Algorithms',
    faculty: 'Meera Krishnan',
    date: '2026-10-24',
    studentsEnrolled: 75,
    studentsEvaluated: 75,
    status: 'RESULTS_PUBLISHED',
  },
  {
    id: 'exam-rec-02',
    examCode: 'EX-CS-303-L1',
    title: 'Operating Systems Kernel Practical Exam',
    subject: 'Operating Systems',
    faculty: 'Priya Raman',
    date: '2026-10-25',
    studentsEnrolled: 60,
    studentsEvaluated: 60,
    status: 'COMPLETED',
  },
  {
    id: 'exam-rec-03',
    examCode: 'EX-CS-302-Q2',
    title: 'Distributed Database Systems Assessment',
    subject: 'Database Systems',
    faculty: 'Arun Kumar',
    date: '2026-10-27',
    studentsEnrolled: 55,
    studentsEvaluated: 38,
    status: 'IN_PROGRESS',
  },
  {
    id: 'exam-rec-04',
    examCode: 'EX-CS-403-M2',
    title: 'Deep Learning & Neural Networks Mid-Sem',
    subject: 'Machine Learning',
    faculty: 'Rahul Varma',
    date: '2026-10-27',
    studentsEnrolled: 45,
    studentsEvaluated: 22,
    status: 'IN_PROGRESS',
  },
  {
    id: 'exam-rec-05',
    examCode: 'EX-CS-402-Q1',
    title: 'Network Security & Cryptography Quiz',
    subject: 'Cyber Security',
    faculty: 'Anitha Devi',
    date: '2026-10-29',
    studentsEnrolled: 40,
    studentsEvaluated: 0,
    status: 'SCHEDULED',
  },
  {
    id: 'exam-rec-06',
    examCode: 'EX-CS-401-M1',
    title: 'AI Knowledge Representation Assessment',
    subject: 'Artificial Intelligence',
    faculty: 'Naveen Prakash',
    date: '2026-10-30',
    studentsEnrolled: 50,
    studentsEvaluated: 0,
    status: 'SCHEDULED',
  },
];

// ─────────────────────────────────────────────────────────────────────────────
// 7. Faculty Performance Rankings (Top 5 Ranked by Average Student Score)
// ─────────────────────────────────────────────────────────────────────────────

export const HOD_TOP_FACULTY_RANKING = [...HOD_DEMO_FACULTY]
  .sort((a, b) => b.averageScore - a.averageScore)
  .slice(0, 5)
  .map((f, index) => ({
    rank: index + 1,
    name: f.name,
    designation: f.designation,
    averageScore: f.averageScore,
    passRate: f.passRate,
    examsConducted: f.examsConducted,
    studentsEvaluated: f.studentsEvaluated,
  }));
