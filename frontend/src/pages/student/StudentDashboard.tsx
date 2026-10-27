import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  Calendar, Award, CheckCircle, ArrowRight, AlertCircle, TrendingUp
} from 'lucide-react';
import { StatCard } from '@/components/cards/StatCard';
import { ExamCard } from '@/components/cards/ExamCard';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { useAuth } from '@/contexts/AuthContext';
import { ExamStatus } from '@/types';
import { formatDate } from '@/utils';
import { AreaChartComponent } from '@/components/charts/Charts';
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

export function StudentDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [exams, setExams] = useState<any[]>([]);
  const [attempts, setAttempts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchDashboardData() {
      try {
        const [examsRes, attemptsRes] = await Promise.all([
          api.get('/v1/exams', { params: { size: 10 } }),
          api.get('/v1/attempts/student', { params: { size: 100 } })
        ]);

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
          endTime: `${exam.scheduledEndDate || exam.scheduledDate}T${exam.endTime}`,
          scheduledDate: exam.scheduledDate,
          scheduledEndDate: exam.scheduledEndDate,
          scheduledStart: exam.scheduledStart,
          scheduledEnd: exam.scheduledEnd,
          serverTime: exam.serverTime,
          scheduleState: exam.scheduleState,
          canStart: exam.canStart,
          accessMessage: exam.accessMessage,
          attemptStatus: exam.attemptStatus,
          attemptId: exam.attemptId,
          remainingSeconds: exam.remainingSeconds,
          duration: exam.durationMinutes,
          totalMarks: exam.totalMarks,
          passingMarks: exam.passingMarks,
          status: exam.scheduleState ? exam.scheduleState.toLowerCase() : mapStatus(exam.status),
          venue: exam.venue || 'N/A',
          enrolledCount: 0,
          maxStudents: 100,
          createdAt: exam.createdAt || '',
          instructions: exam.instructions ? exam.instructions.split('\n') : [],
          isAIMonitored: true,
        }));
        setExams(mappedExams);
        setAttempts(attemptsRes.data.data.content || []);
      } catch (err) {
        toast.error('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    }
    fetchDashboardData();
  }, []);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  const upcomingExams = exams.filter((e) => (e.scheduleState || '').toUpperCase() === 'UPCOMING' || e.status === ExamStatus.UPCOMING);
  const liveExams = exams.filter((e) => (e.scheduleState || '').toUpperCase() === 'AVAILABLE' || (e.scheduleState || '').toUpperCase() === 'IN_PROGRESS' || e.status === ExamStatus.LIVE);
  const completedExams = exams.filter((e) => (e.scheduleState || '').toUpperCase() === 'COMPLETED' || e.status === ExamStatus.COMPLETED);

  const completedAttempts = attempts.filter(
    (a: any) => a.status === 'SUBMITTED' || a.status === 'AUTO_SUBMITTED'
  );

  const avgScore = completedAttempts.length
    ? completedAttempts.reduce((sum: number, a: any) => sum + (a.percentage || 0), 0) / completedAttempts.length
    : 0;

  // Group completed attempts by month for the Score Trend chart
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const trendMap: Record<string, { sum: number; count: number }> = {};
  completedAttempts.forEach((a: any) => {
    const dateStr = a.submittedTime || a.createdAt;
    if (dateStr) {
      const date = new Date(dateStr);
      const monthName = months[date.getMonth()];
      if (!trendMap[monthName]) {
        trendMap[monthName] = { sum: 0, count: 0 };
      }
      trendMap[monthName].sum += (a.percentage || 0);
      trendMap[monthName].count += 1;
    }
  });

  const computedTrend = Object.keys(trendMap)
    .map((month) => ({
      name: month,
      avgScore: Math.round(trendMap[month].sum / trendMap[month].count),
    }))
    .sort((a, b) => months.indexOf(a.name) - months.indexOf(b.name));

  // Fallback trend data if none exists yet
  const chartData = computedTrend.length ? computedTrend : [
    { name: 'No Data', avgScore: 0 }
  ];

  const getGrade = (percentage: number) => {
    if (percentage >= 90) return 'O';
    if (percentage >= 80) return 'A+';
    if (percentage >= 70) return 'A';
    if (percentage >= 60) return 'B+';
    if (percentage >= 50) return 'B';
    return 'C';
  };

  return (
    <div className="space-y-6">
      {/* Welcome banner */}
      <motion.div
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        className="rounded-2xl bg-gradient-primary p-6 text-white relative overflow-hidden"
      >
        <div className="absolute right-0 top-0 w-48 h-48 rounded-full bg-white/5 -translate-y-12 translate-x-12" />
        <div className="absolute right-12 bottom-0 w-24 h-24 rounded-full bg-white/5 translate-y-8" />
        <div className="relative z-10">
          <p className="text-blue-200 text-sm font-medium">Good morning 👋</p>
          <h1 className="font-display font-bold text-2xl mt-1">
            {user?.name ?? 'Student'}
          </h1>
          <p className="text-blue-100 text-sm mt-1">
            You have <span className="font-bold text-white">{upcomingExams.length} upcoming</span> and{' '}
            <span className="font-bold text-white">{liveExams.length} live</span> exams.
          </p>
        </div>
      </motion.div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard
          title="Upcoming Exams"
          value={upcomingExams.length}
          icon={Calendar}
          iconBg="bg-primary/10"
          iconColor="text-primary"
          delay={0}
        />
        <StatCard
          title="Live Now"
          value={liveExams.length}
          icon={AlertCircle}
          iconBg="bg-danger/10"
          iconColor="text-danger"
          delay={0.05}
        />
        <StatCard
          title="Completed"
          value={completedExams.length}
          icon={CheckCircle}
          iconBg="bg-success/10"
          iconColor="text-success"
          delay={0.1}
        />
        <StatCard
          title="Avg Score"
          value={`${avgScore.toFixed(1)}%`}
          icon={TrendingUp}
          iconBg="bg-secondary/10"
          iconColor="text-secondary"
          change={computedTrend.length ? 5.2 : undefined}
          changeLabel="vs last month"
          delay={0.15}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Exams */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-display font-semibold text-foreground text-lg">My Exams</h2>
            <button
              onClick={() => navigate(ROUTES.STUDENT.EXAMS)}
              className="text-sm text-primary hover:underline flex items-center gap-1"
            >
              View all <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>
          <div className="space-y-4">
            {exams.length === 0 ? (
              <Card className="p-6 text-center text-muted-foreground">No exams scheduled.</Card>
            ) : (
              exams.slice(0, 3).map((exam, i) => (
                <ExamCard
                  key={exam.id}
                  exam={exam}
                  role="student"
                  delay={i * 0.05}
                  onJoin={() => navigate(ROUTES.STUDENT.JOIN_EXAM.replace(':examId', exam.id))}
                  onView={(clickedExam) => navigate(
                    clickedExam.status === ExamStatus.UPCOMING
                      ? ROUTES.STUDENT.EXAM_DETAIL.replace(':examId', clickedExam.id)
                      : ROUTES.STUDENT.RESULTS
                  )}
                />
              ))
            )}
          </div>
        </div>

        {/* Right column */}
        <div className="space-y-4">
          {/* Recent results */}
          <Card>
            <CardHeader
              title="Recent Results"
              icon={<Award className="h-4 w-4" />}
              action={
                <button onClick={() => navigate(ROUTES.STUDENT.RESULTS)} className="text-xs text-primary hover:underline">
                  View all
                </button>
              }
            />
            <div className="space-y-3">
              {completedAttempts.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-4">No results available yet.</p>
              ) : (
                completedAttempts.slice(0, 3).map((attempt) => {
                  const percentage = attempt.percentage || 0;
                  return (
                    <div
                      key={attempt.id}
                      className="flex items-center justify-between p-3 rounded-lg bg-muted/40 hover:bg-muted transition-colors cursor-pointer"
                      onClick={() => navigate(ROUTES.STUDENT.RESULTS)}
                    >
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-foreground truncate">
                          {attempt.examSession?.exam?.examTitle || 'Exam Attempt'}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {formatDate(attempt.submittedTime || attempt.createdAt)}
                        </p>
                      </div>
                      <div className="text-right flex-shrink-0 ml-3">
                        <p className="font-display font-bold text-foreground text-base">
                          {percentage.toFixed(0)}%
                        </p>
                        <Badge
                          variant={percentage >= 75 ? 'success' : percentage >= 50 ? 'warning' : 'danger'}
                        >
                          {getGrade(percentage)}
                        </Badge>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </Card>

          {/* Performance trend */}
          <Card>
            <CardHeader title="Score Trend" icon={<TrendingUp className="h-4 w-4" />} />
            <AreaChartComponent
              data={chartData}
              areas={[{ key: 'avgScore', label: 'Avg Score' }]}
              xKey="name"
              height={160}
            />
          </Card>
        </div>
      </div>
    </div>
  );
}
