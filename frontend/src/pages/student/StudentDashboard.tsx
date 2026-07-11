import React from 'react';
import { motion } from 'framer-motion';
import {
  BookOpen, Calendar, Award, Clock, TrendingUp, CheckCircle,
  ArrowRight, AlertCircle,
} from 'lucide-react';
import { StatCard } from '@/components/cards/StatCard';
import { ExamCard } from '@/components/cards/ExamCard';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { useAuth } from '@/contexts/AuthContext';
import { mockExams, mockResults, mockExamTrend } from '@/services/mockData';
import { ExamStatus } from '@/types';
import { formatDate, formatPercentage } from '@/utils';
import { AreaChartComponent } from '@/components/charts/Charts';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants';

export function StudentDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const upcomingExams = mockExams.filter((e) => e.status === ExamStatus.UPCOMING);
  const liveExams = mockExams.filter((e) => e.status === ExamStatus.LIVE);
  const completedExams = mockExams.filter((e) => e.status === ExamStatus.COMPLETED);
  const avgScore = mockResults.length
    ? mockResults.reduce((sum, r) => sum + r.percentage, 0) / mockResults.length
    : 0;

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
            <span className="font-bold text-white">{liveExams.length} live</span> exams today.
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
          change={5.2}
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
            {mockExams.slice(0, 3).map((exam, i) => (
              <ExamCard
                key={exam.id}
                exam={exam}
                role="student"
                delay={i * 0.05}
                onJoin={() => navigate(ROUTES.STUDENT.JOIN_EXAM.replace(':examId', exam.id))}
                onView={() => navigate(ROUTES.STUDENT.RESULTS)}
              />
            ))}
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
              {mockResults.map((result) => (
                <div
                  key={result.id}
                  className="flex items-center justify-between p-3 rounded-lg bg-muted/40 hover:bg-muted transition-colors cursor-pointer"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">{result.examTitle}</p>
                    <p className="text-xs text-muted-foreground">{formatDate(result.submittedAt)}</p>
                  </div>
                  <div className="text-right flex-shrink-0 ml-3">
                    <p className="font-display font-bold text-foreground text-base">
                      {result.percentage}%
                    </p>
                    <Badge
                      variant={result.percentage >= 75 ? 'success' : result.percentage >= 50 ? 'warning' : 'danger'}
                    >
                      {result.grade}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {/* Performance trend */}
          <Card>
            <CardHeader title="Score Trend" icon={<TrendingUp className="h-4 w-4" />} />
            <AreaChartComponent
              data={mockExamTrend}
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
