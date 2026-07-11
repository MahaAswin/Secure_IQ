import React from 'react';
import { motion } from 'framer-motion';
import { Clock, Users, MapPin, BookOpen, Calendar } from 'lucide-react';
import { Exam, ExamStatus } from '@/types';
import { cn, formatDateTime, formatDuration } from '@/utils';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';

interface ExamCardProps {
  exam: Exam;
  role?: 'student' | 'faculty';
  onJoin?: (exam: Exam) => void;
  onView?: (exam: Exam) => void;
  onMonitor?: (exam: Exam) => void;
  delay?: number;
  className?: string;
}

const statusVariantMap: Record<ExamStatus, 'live' | 'upcoming' | 'completed' | 'cancelled'> = {
  [ExamStatus.LIVE]: 'live',
  [ExamStatus.UPCOMING]: 'upcoming',
  [ExamStatus.COMPLETED]: 'completed',
  [ExamStatus.CANCELLED]: 'cancelled',
};

const statusLabelMap: Record<ExamStatus, string> = {
  [ExamStatus.LIVE]: 'Live Now',
  [ExamStatus.UPCOMING]: 'Upcoming',
  [ExamStatus.COMPLETED]: 'Completed',
  [ExamStatus.CANCELLED]: 'Cancelled',
};

export function ExamCard({ exam, role = 'student', onJoin, onView, onMonitor, delay = 0, className }: ExamCardProps) {
  const isLive = exam.status === ExamStatus.LIVE;
  const isUpcoming = exam.status === ExamStatus.UPCOMING;
  const isCompleted = exam.status === ExamStatus.COMPLETED;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay }}
      className={cn(
        'rounded-xl border bg-card shadow-soft overflow-hidden transition-all duration-200 hover:shadow-card',
        isLive ? 'border-danger/30' : 'border-border',
        className
      )}
    >
      {/* Top accent */}
      {isLive && (
        <div className="h-1 bg-gradient-to-r from-danger to-danger-600" />
      )}
      {isUpcoming && (
        <div className="h-1 bg-gradient-to-r from-primary to-secondary" />
      )}

      <div className="p-5">
        {/* Header */}
        <div className="flex items-start justify-between gap-3 mb-3">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-mono text-muted-foreground">{exam.code}</span>
              <Badge variant={statusVariantMap[exam.status]} dot pulse={isLive}>
                {statusLabelMap[exam.status]}
              </Badge>
            </div>
            <h3 className="font-display font-semibold text-foreground text-base line-clamp-1">
              {exam.title}
            </h3>
            <p className="text-sm text-muted-foreground mt-0.5">{exam.facultyName}</p>
          </div>
          {exam.isAIMonitored && (
            <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-secondary/10 text-secondary-600 dark:text-secondary-400 font-medium whitespace-nowrap">
              AI Monitor
            </span>
          )}
        </div>

        {/* Details */}
        <div className="grid grid-cols-2 gap-2 mb-4">
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Calendar className="h-3.5 w-3.5 flex-shrink-0" />
            <span>{formatDateTime(exam.startTime)}</span>
          </div>
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Clock className="h-3.5 w-3.5 flex-shrink-0" />
            <span>{formatDuration(exam.duration)}</span>
          </div>
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <MapPin className="h-3.5 w-3.5 flex-shrink-0" />
            <span className="truncate">{exam.venue}</span>
          </div>
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Users className="h-3.5 w-3.5 flex-shrink-0" />
            <span>{exam.enrolledCount}/{exam.maxStudents} students</span>
          </div>
        </div>

        {/* Marks */}
        <div className="flex items-center gap-2 mb-4">
          <BookOpen className="h-3.5 w-3.5 text-muted-foreground" />
          <span className="text-xs text-muted-foreground">
            {exam.totalMarks} marks · {exam.passingMarks} to pass
          </span>
        </div>

        {/* Enrollment bar */}
        <div className="mb-4">
          <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
            <div
              className="h-full rounded-full bg-gradient-to-r from-primary to-secondary transition-all duration-500"
              style={{ width: `${(exam.enrolledCount / exam.maxStudents) * 100}%` }}
            />
          </div>
        </div>

        {/* Actions */}
        <div className="flex gap-2">
          {role === 'student' ? (
            <>
              {isLive && onJoin && (
                <Button variant="primary" size="sm" className="flex-1" onClick={() => onJoin(exam)}>
                  Join Exam
                </Button>
              )}
              {(isUpcoming || isCompleted) && onView && (
                <Button variant="outline" size="sm" className="flex-1" onClick={() => onView(exam)}>
                  {isCompleted ? 'View Result' : 'View Details'}
                </Button>
              )}
            </>
          ) : (
            <>
              {(isLive || isUpcoming) && onMonitor && (
                <Button variant={isLive ? 'primary' : 'outline'} size="sm" onClick={() => onMonitor(exam)}>
                  {isLive ? 'Monitor Live' : 'Prepare'}
                </Button>
              )}
              {onView && (
                <Button variant="ghost" size="sm" onClick={() => onView(exam)}>
                  View
                </Button>
              )}
            </>
          )}
        </div>
      </div>
    </motion.div>
  );
}
