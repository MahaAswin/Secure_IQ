import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  BookOpen, Users, BarChart3, TrendingUp, Plus, MonitorPlay,
} from 'lucide-react';
import { StatCard } from '@/components/cards/StatCard';
import { ExamCard } from '@/components/cards/ExamCard';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { BarChartComponent, AreaChartComponent } from '@/components/charts/Charts';
import { useAuth } from '@/contexts/AuthContext';
import { ExamStatus } from '@/types';
import { formatDate } from '@/utils';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants';
import api from '@/services/api';
import toast from 'react-hot-toast';

const mapStatus = (backendStatus: string): ExamStatus => {
  switch (backendStatus.toUpperCase()) {
    case 'ACTIVE':
      return ExamStatus.LIVE;
    case 'COMPLETED':
      return ExamStatus.COMPLETED;
    case 'CANCELLED':
      return ExamStatus.CANCELLED;
    case 'PUBLISHED':
    case 'DRAFT':
    default:
      return ExamStatus.UPCOMING;
  }
};

export function FacultyDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [dashboardData, setDashboardData] = useState<any>(null);
  const [exams, setExams] = useState<any[]>([]);
  const [violations, setViolations] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchData() {
      try {
        const [dashRes, examsRes, violationsRes] = await Promise.all([
          api.get('/v1/faculties/dashboard'),
          api.get('/v1/exams', { params: { size: 10 } }),
          api.get('/v1/violations', { params: { size: 5 } }),
        ]);

        setDashboardData(dashRes.data.data);
        setViolations(violationsRes.data.data.content || []);

        const backendExams = examsRes.data.data.content || [];
        const mappedExams = backendExams.map((exam: any) => ({
          id: exam.id.toString(),
          title: exam.examTitle,
          subject: exam.subject ? `${exam.subject.subjectCode} - ${exam.subject.subjectName}` : 'N/A',
          code: exam.examCode,
          description: exam.description || '',
          facultyId: exam.faculty?.id?.toString() || '',
          facultyName: exam.faculty?.name || 'N/A',
          department: exam.department?.name || 'N/A',
          startTime: `${exam.scheduledDate}T${exam.startTime}`,
          endTime: `${exam.scheduledDate}T${exam.endTime}`,
          duration: exam.durationMinutes,
          totalMarks: exam.totalMarks,
          passingMarks: exam.passingMarks,
          status: mapStatus(exam.status),
          venue: exam.venue || 'N/A',
          enrolledCount: 0,
          maxStudents: 100,
          createdAt: exam.createdAt || '',
          instructions: exam.instructions ? exam.instructions.split('\n') : [],
          isAIMonitored: true,
        }));
        setExams(mappedExams);
      } catch (err) {
        toast.error('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  const liveExams = exams.filter((e) => e.status === ExamStatus.LIVE);
  const totalStudents = dashboardData?.assignedStudentsCount || 0;
  const myExamsCount = exams.length;

  // Compute dummy/fallback trends based on faculty's exams
  const chartData = exams.length
    ? exams.slice(0, 5).map((exam) => ({
        name: exam.code.split('-')[0],
        students: 45,
        avgScore: 75,
      }))
    : [
        { name: 'No Data', students: 0, avgScore: 0 }
      ];

  const deptPerformanceData = [
    { name: user?.department || 'CS', avgScore: 78, passRate: 88 },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">Faculty Dashboard</h1>
          <p className="text-muted-foreground text-sm mt-1">Welcome back, {user?.name}</p>
        </div>
        <Button
          variant="primary"
          leftIcon={<Plus className="h-4 w-4" />}
          onClick={() => navigate(ROUTES.FACULTY.CREATE_EXAM)}
        >
          Create Exam
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard title="My Exams" value={myExamsCount} icon={BookOpen} delay={0} />
        <StatCard title="Total Students" value={totalStudents} icon={Users} format="number" iconBg="bg-secondary/10" iconColor="text-secondary" delay={0.05} />
        <StatCard title="Live Exams" value={liveExams.length} icon={MonitorPlay} iconBg="bg-danger/10" iconColor="text-danger" delay={0.1} />
        <StatCard title="Avg Score" value="78.5%" icon={TrendingUp} iconBg="bg-success/10" iconColor="text-success" delay={0.15} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main */}
        <div className="lg:col-span-2 space-y-5">
          {/* Live monitoring alert */}
          {liveExams.length > 0 && (
            <motion.div
              initial={{ opacity: 0, scale: 0.98 }}
              animate={{ opacity: 1, scale: 1 }}
              className="rounded-xl border border-danger/30 bg-danger/5 p-4 flex items-center justify-between gap-4"
            >
              <div className="flex items-center gap-3">
                <div className="h-2.5 w-2.5 rounded-full bg-danger animate-pulse" />
                <div>
                  <p className="text-sm font-semibold text-foreground">{liveExams.length} Exam Live Now</p>
                  <p className="text-xs text-muted-foreground">{totalStudents} students registered</p>
                </div>
              </div>
              <Button variant="danger" size="sm" leftIcon={<MonitorPlay className="h-4 w-4" />} onClick={() => navigate(ROUTES.FACULTY.MONITORING)}>
                Monitor
              </Button>
            </motion.div>
          )}

          {/* Exam list */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h2 className="font-display font-semibold text-foreground">My Exams</h2>
              <button onClick={() => navigate(ROUTES.FACULTY.EXAMS)} className="text-sm text-primary hover:underline">View all</button>
            </div>
            <div className="space-y-3">
              {exams.length === 0 ? (
                <Card className="p-6 text-center text-muted-foreground">No exams created yet.</Card>
              ) : (
                exams.slice(0, 3).map((exam, i) => (
                  <ExamCard
                    key={exam.id}
                    exam={exam}
                    role="faculty"
                    delay={i * 0.05}
                    onMonitor={() => navigate(ROUTES.FACULTY.MONITORING)}
                    onView={() => navigate(ROUTES.FACULTY.EXAM_DETAIL.replace(':examId', exam.id))}
                  />
                ))
              )}
            </div>
          </div>

          {/* Score trend */}
          <Card>
            <CardHeader title="Exam Performance Trend" icon={<BarChart3 className="h-4 w-4" />} />
            <AreaChartComponent
              data={chartData}
              areas={[
                { key: 'avgScore', label: 'Avg Score', color: '#2563EB' },
                { key: 'students', label: 'Students', color: '#7C3AED' },
              ]}
              xKey="name"
              height={200}
              showLegend
            />
          </Card>
        </div>

        {/* Right */}
        <div className="space-y-4">
          {/* Recent Violations */}
          <Card>
            <CardHeader
              title="Recent Violations"
              icon={<MonitorPlay className="h-4 w-4" />}
            />
            <div className="space-y-3">
              {violations.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-4">No violations recorded.</p>
              ) : (
                violations.map((violation) => (
                  <div key={violation.id} className="flex items-center justify-between gap-2 p-2.5 rounded-lg bg-muted/40">
                    <div>
                      <p className="text-sm font-medium text-foreground">
                        {violation.studentExamAttempt?.student?.user
                          ? `${violation.studentExamAttempt.student.user.firstName || ''} ${violation.studentExamAttempt.student.user.lastName || ''}`.trim()
                          : violation.studentExamAttempt?.student?.rollNumber || violation.student?.name || 'Student'}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {violation.violationCode} · {formatDate(violation.createdAt)}
                      </p>
                    </div>
                    <Badge variant={violation.severity === 'CRITICAL' || violation.severity === 'HIGH' ? 'danger' : 'warning'}>
                      {violation.severity}
                    </Badge>
                  </div>
                ))
              )}
            </div>
          </Card>

          {/* Dept performance */}
          <Card>
            <CardHeader title="Dept. Performance" icon={<BarChart3 className="h-4 w-4" />} />
            <BarChartComponent
              data={deptPerformanceData}
              bars={[{ key: 'avgScore', label: 'Avg Score' }, { key: 'passRate', label: 'Pass Rate' }]}
              xKey="name"
              height={160}
              showLegend
            />
          </Card>
        </div>
      </div>
    </div>
  );
}
