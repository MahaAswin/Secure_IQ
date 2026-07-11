import React from 'react';
import { motion } from 'framer-motion';
import {
  BookOpen, Users, BarChart3, TrendingUp, Plus, MonitorPlay, FileText, Clock,
} from 'lucide-react';
import { StatCard } from '@/components/cards/StatCard';
import { ExamCard } from '@/components/cards/ExamCard';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { BarChartComponent, AreaChartComponent } from '@/components/charts/Charts';
import { useAuth } from '@/contexts/AuthContext';
import { mockExams, mockResults, mockExamTrend, mockDeptPerformance, mockMonitoringSessions } from '@/services/mockData';
import { ExamStatus } from '@/types';
import { formatRelativeTime, formatPercentage, getRiskColor, getRiskLabel } from '@/utils';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants';

export function FacultyDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const myExams = mockExams.filter((e) => e.facultyId === 'fac-001');
  const liveExams = myExams.filter((e) => e.status === ExamStatus.LIVE);
  const upcomingExams = myExams.filter((e) => e.status === ExamStatus.UPCOMING);
  const totalStudents = myExams.reduce((s, e) => s + e.enrolledCount, 0);
  const avgScore = mockResults.length ? mockResults.reduce((s, r) => s + r.percentage, 0) / mockResults.length : 0;

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
        <StatCard title="My Exams" value={myExams.length} icon={BookOpen} delay={0} />
        <StatCard title="Total Students" value={totalStudents} icon={Users} format="number" iconBg="bg-secondary/10" iconColor="text-secondary" delay={0.05} />
        <StatCard title="Live Exams" value={liveExams.length} icon={MonitorPlay} iconBg="bg-danger/10" iconColor="text-danger" delay={0.1} />
        <StatCard title="Avg Score" value={`${avgScore.toFixed(1)}%`} icon={TrendingUp} change={3.4} changeLabel="vs last exam" iconBg="bg-success/10" iconColor="text-success" delay={0.15} />
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
                  <p className="text-xs text-muted-foreground">{mockMonitoringSessions.length} students active</p>
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
              {myExams.slice(0, 3).map((exam, i) => (
                <ExamCard
                  key={exam.id}
                  exam={exam}
                  role="faculty"
                  delay={i * 0.05}
                  onMonitor={() => navigate(ROUTES.FACULTY.MONITORING)}
                  onView={() => navigate(ROUTES.FACULTY.EXAM_DETAIL.replace(':examId', exam.id))}
                />
              ))}
            </div>
          </div>

          {/* Score trend */}
          <Card>
            <CardHeader title="Exam Performance Trend" icon={<BarChart3 className="h-4 w-4" />} />
            <AreaChartComponent
              data={mockExamTrend}
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
          {/* Active monitoring */}
          <Card>
            <CardHeader
              title="Active Students"
              icon={<MonitorPlay className="h-4 w-4" />}
              action={<Badge variant="live" dot pulse>Live</Badge>}
            />
            <div className="space-y-3">
              {mockMonitoringSessions.map((session) => (
                <div key={session.id} className="flex items-center justify-between gap-2 p-2.5 rounded-lg bg-muted/40">
                  <div>
                    <p className="text-sm font-medium text-foreground">{session.studentName}</p>
                    <p className="text-xs text-muted-foreground">{session.flagCount} flags · {formatRelativeTime(session.lastActivity)}</p>
                  </div>
                  <span className={`text-xs font-bold ${getRiskColor(session.riskScore)}`}>
                    {session.riskScore}%
                  </span>
                </div>
              ))}
            </div>
          </Card>

          {/* Dept performance */}
          <Card>
            <CardHeader title="Dept. Performance" icon={<BarChart3 className="h-4 w-4" />} />
            <BarChartComponent
              data={mockDeptPerformance}
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
