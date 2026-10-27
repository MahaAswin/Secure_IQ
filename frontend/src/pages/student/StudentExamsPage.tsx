import React, { useState, useEffect } from 'react';
import { BookOpen, Plus, RefreshCw } from 'lucide-react';
import { ExamCard } from '@/components/cards/ExamCard';
import { SearchBar } from '@/components/common/SearchBar';
import { EmptyState } from '@/components/common/EmptyState';
import { Button } from '@/components/common/Button';
import { ExamStatus } from '@/types';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants';
import { cn } from '@/utils';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/services/api';
import toast from 'react-hot-toast';

const mapStatus = (backendStatus: string): ExamStatus => {
  switch (backendStatus?.toUpperCase()) {
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

const STATUS_FILTERS = [
  { label: 'All', value: 'all' },
  { label: 'Available Now', value: 'available' },
  { label: 'Upcoming', value: 'upcoming' },
  { label: 'Completed', value: 'completed' },
  { label: 'Expired', value: 'expired' },
];

export function StudentExamsPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [exams, setExams] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [filter, setFilter] = useState<string>('all');
  const [query, setQuery] = useState('');

  const fetchExams = async (isManualRefresh = false) => {
    if (isManualRefresh) setRefreshing(true);
    try {
      const response = await api.get('/v1/exams', {
        params: { size: 100 }
      });
      const backendExams = response.data.data?.content || [];

      const mappedExams = backendExams.map((exam: any) => {
        const facultyDisplayName = exam.faculty?.name?.trim() || exam.faculty?.employeeId || 'Faculty';
        return {
          id: exam.id.toString(),
          title: exam.examTitle,
          subject: exam.subject ? `${exam.subject.subjectCode} - ${exam.subject.subjectName}` : 'N/A',
          code: exam.examCode,
          description: exam.description || '',
          facultyId: exam.faculty?.id?.toString() || '',
          facultyName: facultyDisplayName,
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
          rawStatus: exam.status,
          venue: exam.venue || 'N/A',
          enrolledCount: 0,
          maxStudents: 100,
          createdAt: exam.createdAt || '',
          instructions: exam.instructions ? exam.instructions.split('\n') : [],
          isAIMonitored: true,
        };
      });

      setExams(mappedExams);
    } catch (err: any) {
      toast.error('Failed to load exams from backend');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchExams();
  }, []);

  const filtered = exams.filter((exam) => {
    const st = (exam.scheduleState || exam.status || '').toLowerCase();
    const matchStatus = filter === 'all' ||
      (filter === 'available' && (st === 'available' || st === 'live' || st === 'in_progress')) ||
      (filter === 'upcoming' && st === 'upcoming') ||
      (filter === 'completed' && st === 'completed') ||
      (filter === 'expired' && (st === 'expired' || st === 'cancelled'));
    const matchQuery = !query ||
      exam.title.toLowerCase().includes(query.toLowerCase()) ||
      exam.code.toLowerCase().includes(query.toLowerCase()) ||
      exam.subject.toLowerCase().includes(query.toLowerCase());
    return matchStatus && matchQuery;
  });

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  const role = user?.role === 'faculty' ? 'faculty' : 'student';

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">
            {role === 'faculty' ? 'Exams' : 'My Scheduled Exams'}
          </h1>
          <p className="text-muted-foreground text-sm mt-1">
            {role === 'faculty' ? 'All examinations managed in PostgreSQL database' : 'All your assigned examinations and upcoming assessments'}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            leftIcon={<RefreshCw className={`h-3.5 w-3.5 ${refreshing ? 'animate-spin' : ''}`} />}
            onClick={() => fetchExams(true)}
            disabled={refreshing}
          >
            Refresh
          </Button>
          {role === 'faculty' && (
            <Button
              variant="primary"
              size="sm"
              leftIcon={<Plus className="h-4 w-4" />}
              onClick={() => navigate(ROUTES.FACULTY.CREATE_EXAM)}
            >
              Create Exam
            </Button>
          )}
        </div>
      </div>

      {/* Filters and search */}
      <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
        <SearchBar onSearch={setQuery} placeholder="Search by exam name or code..." className="flex-1 max-w-sm" />
        <div className="flex gap-2 overflow-x-auto pb-1 sm:pb-0">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => setFilter(f.value)}
              className={cn(
                'px-3 py-1.5 rounded-lg text-sm font-medium transition-colors whitespace-nowrap',
                filter === f.value
                  ? 'bg-primary text-white'
                  : 'bg-muted text-muted-foreground hover:text-foreground'
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={BookOpen}
          title="No exams found"
          description={role === 'faculty' ? 'You have not created any exams yet, or none match your filters.' : 'No exams assigned to your department and semester.'}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map((exam, i) => (
            <ExamCard
              key={exam.id}
              exam={exam}
              role={role}
              delay={i * 0.05}
              onMonitor={() => navigate(ROUTES.FACULTY.MONITORING)}
              onJoin={() => navigate(ROUTES.STUDENT.JOIN_EXAM.replace(':examId', exam.id))}
              onView={(clickedExam) => navigate(
                role === 'faculty'
                  ? ROUTES.FACULTY.EXAM_DETAIL.replace(':examId', clickedExam.id)
                  : clickedExam.status === ExamStatus.UPCOMING
                  ? ROUTES.STUDENT.EXAM_DETAIL.replace(':examId', clickedExam.id)
                  : ROUTES.STUDENT.RESULTS
              )}
            />
          ))}
        </div>
      )}
    </div>
  );
}
