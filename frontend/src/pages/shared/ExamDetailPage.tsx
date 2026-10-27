import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Clock, MapPin, Calendar, BookOpen, Award, AlertTriangle, ChevronLeft, ShieldAlert,
  Edit2, Trash2, CheckCircle2, X
} from 'lucide-react';
import { Button } from '@/components/common/Button';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { Input, Textarea, Select } from '@/components/forms/FormFields';
import { ConfirmDialog } from '@/components/dialogs/Dialog';
import { ROUTES } from '@/constants';
import { formatDateTime, formatDuration } from '@/utils';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/services/api';
import toast from 'react-hot-toast';

export function ExamDetailPage() {
  const { examId } = useParams<{ examId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [exam, setExam] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Edit form state
  const [editForm, setEditForm] = useState({
    examTitle: '',
    description: '',
    venue: '',
    totalMarks: 100,
    passingMarks: 40,
    durationMinutes: 120,
    status: 'PUBLISHED',
  });

  async function fetchExam() {
    try {
      const response = await api.get(`/v1/exams/${examId}`);
      const backendExam = response.data.data;
      const mappedExam = {
        id: backendExam.id.toString(),
        rawId: backendExam.id,
        title: backendExam.examTitle,
        subject: backendExam.subject ? `${backendExam.subject.subjectCode} - ${backendExam.subject.subjectName}` : 'N/A',
        subjectObj: backendExam.subject,
        code: backendExam.examCode,
        description: backendExam.description || '',
        facultyId: backendExam.faculty?.id?.toString() || '',
        facultyName: backendExam.faculty?.name || 'N/A',
        facultyObj: backendExam.faculty,
        department: backendExam.department?.name || 'N/A',
        departmentObj: backendExam.department,
        semester: backendExam.semester || 1,
        examType: backendExam.examType || 'MID_SEMESTER',
        scheduledDate: backendExam.scheduledDate,
        scheduledEndDate: backendExam.scheduledEndDate,
        scheduledStart: backendExam.scheduledStart,
        scheduledEnd: backendExam.scheduledEnd,
        serverTime: backendExam.serverTime,
        scheduleState: backendExam.scheduleState,
        canStart: backendExam.canStart,
        accessMessage: backendExam.accessMessage,
        attemptStatus: backendExam.attemptStatus,
        attemptId: backendExam.attemptId,
        remainingSeconds: backendExam.remainingSeconds,
        startTimeRaw: backendExam.startTime,
        endTimeRaw: backendExam.endTime,
        startTime: backendExam.scheduledStart || `${backendExam.scheduledDate}T${backendExam.startTime}`,
        endTime: backendExam.scheduledEnd || `${backendExam.scheduledEndDate || backendExam.scheduledDate}T${backendExam.endTime}`,
        duration: backendExam.durationMinutes,
        totalMarks: backendExam.totalMarks,
        passingMarks: backendExam.passingMarks,
        status: backendExam.scheduleState ? backendExam.scheduleState.toLowerCase() : (backendExam.status ? backendExam.status.toLowerCase() : 'upcoming'),
        rawStatus: backendExam.status,
        venue: backendExam.venue || 'N/A',
        enrolledCount: 0,
        maxStudents: 100,
        createdAt: backendExam.createdAt || '',
        instructions: backendExam.instructions ? backendExam.instructions.split('\n') : [],
        isAIMonitored: true,
      };
      setExam(mappedExam);
      setEditForm({
        examTitle: backendExam.examTitle,
        description: backendExam.description || '',
        venue: backendExam.venue || '',
        totalMarks: backendExam.totalMarks || 100,
        passingMarks: backendExam.passingMarks || 40,
        durationMinutes: backendExam.durationMinutes || 120,
        status: backendExam.status || 'PUBLISHED',
      });
    } catch (err: any) {
      toast.error('Failed to load exam details');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchExam();
  }, [examId]);

  const handleDelete = async () => {
    try {
      setSubmitting(true);
      await api.delete(`/v1/exams/${examId}`);
      toast.success('Exam deleted successfully');
      setDeleteModalOpen(false);
      navigate(user?.role === 'faculty' ? ROUTES.FACULTY.EXAMS : ROUTES.STUDENT.EXAMS);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to delete exam';
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (editForm.passingMarks > editForm.totalMarks) {
      toast.error('Passing marks cannot exceed total marks');
      return;
    }
    try {
      setSubmitting(true);
      const payload = {
        examCode: exam.code,
        examTitle: editForm.examTitle,
        description: editForm.description,
        subjectId: exam.subjectObj?.id,
        facultyId: exam.facultyObj?.id,
        departmentId: exam.departmentObj?.id,
        semester: exam.semester,
        examType: exam.examType,
        totalMarks: editForm.totalMarks,
        durationMinutes: editForm.durationMinutes,
        passingMarks: editForm.passingMarks,
        scheduledDate: exam.scheduledDate,
        startTime: exam.startTimeRaw,
        endTime: exam.endTimeRaw,
        venue: editForm.venue,
        instructions: exam.instructions.join('\n'),
        status: editForm.status,
      };

      await api.put(`/v1/exams/${examId}`, payload);
      toast.success('Exam updated successfully');
      setEditModalOpen(false);
      fetchExam();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to update exam';
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  if (!exam) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <h2 className="text-xl font-bold text-foreground">Exam not found</h2>
        <Button variant="primary" className="mt-4" onClick={() => navigate(-1)}>
          Go Back
        </Button>
      </div>
    );
  }

  const role = user?.role || 'student';
  const isFacultyOrAdmin = role === 'faculty' || role === 'admin' || role === 'hod';
  const isLive = exam.status === 'live' || exam.status === 'active';
  const isUpcoming = exam.status === 'upcoming' || exam.status === 'published';
  const isDraft = exam.status === 'draft';
  const isCompleted = exam.status === 'completed';

  const statusVariantMap: Record<string, 'live' | 'upcoming' | 'completed' | 'cancelled'> = {
    live: 'live',
    active: 'live',
    upcoming: 'upcoming',
    published: 'upcoming',
    draft: 'cancelled',
    completed: 'completed',
    cancelled: 'cancelled',
  };

  const statusLabelMap: Record<string, string> = {
    live: 'Live Now',
    active: 'Live Now',
    upcoming: 'Upcoming',
    published: 'Published',
    draft: 'Draft (Faculty Only)',
    completed: 'Completed',
    cancelled: 'Cancelled',
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Top action bar */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          <ChevronLeft className="h-4 w-4" />
          Back to Exams
        </button>

        {isFacultyOrAdmin && (
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              leftIcon={<Edit2 className="h-3.5 w-3.5" />}
              onClick={() => setEditModalOpen(true)}
            >
              Edit Exam
            </Button>
            <Button
              variant="danger"
              size="sm"
              leftIcon={<Trash2 className="h-3.5 w-3.5" />}
              onClick={() => setDeleteModalOpen(true)}
            >
              Delete Exam
            </Button>
          </div>
        )}
      </div>

      {/* Header card */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        className="rounded-2xl border bg-card shadow-soft p-6 space-y-4"
      >
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b pb-4">
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <span className="text-xs font-mono font-bold bg-muted px-2 py-0.5 rounded text-muted-foreground">{exam.code}</span>
              <Badge variant={statusVariantMap[exam.status] || 'upcoming'} dot={isLive} pulse={isLive}>
                {statusLabelMap[exam.status] || 'Upcoming'}
              </Badge>
            </div>
            <h1 className="font-display font-bold text-foreground text-2xl">{exam.title}</h1>
            <p className="text-muted-foreground text-sm mt-1">{exam.subject}</p>
          </div>
          {exam.isAIMonitored && (
            <div className="self-start sm:self-center flex items-center gap-1.5 px-3 py-1 rounded-full bg-secondary/10 text-secondary-600 dark:text-secondary-400 font-semibold text-xs border border-secondary/20">
              <ShieldAlert className="h-4 w-4" />
              AI Monitored
            </div>
          )}
        </div>

        {/* Overview grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 py-2">
          <div>
            <p className="text-xs text-muted-foreground">Department</p>
            <p className="text-sm font-semibold text-foreground mt-0.5">{exam.department}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Semester</p>
            <p className="text-sm font-semibold text-foreground mt-0.5">Semester {exam.semester}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Faculty In-charge</p>
            <p className="text-sm font-semibold text-foreground mt-0.5">{exam.facultyName}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Exam Type</p>
            <p className="text-sm font-semibold text-foreground mt-0.5 capitalize">{exam.examType.replace(/_/g, ' ')}</p>
          </div>
        </div>
      </motion.div>

      {/* Main content grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-2 space-y-6">
          {/* Description */}
          {exam.description && (
            <Card>
              <CardHeader title="Description" icon={<BookOpen className="h-4 w-4" />} />
              <p className="text-sm text-foreground leading-relaxed">{exam.description}</p>
            </Card>
          )}

          {/* Instructions */}
          <Card>
            <CardHeader title="Exam Instructions" icon={<BookOpen className="h-4 w-4" />} />
            {exam.instructions.length === 0 ? (
              <p className="text-sm text-muted-foreground">No specific instructions provided.</p>
            ) : (
              <div className="space-y-3">
                {exam.instructions.map((inst: string, i: number) => (
                  <div key={i} className="flex gap-3 text-sm text-foreground">
                    <span className="font-bold text-primary w-5">{i + 1}.</span>
                    <span className="flex-1 leading-relaxed">{inst}</span>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>

        {/* Sidebar details */}
        <div className="space-y-6">
          {/* Schedule card */}
          <Card className="p-5 space-y-4">
            <h3 className="font-display font-semibold text-foreground text-sm border-b pb-2">Schedule & Venue</h3>
            <div className="space-y-3.5">
              <div className="flex items-start gap-2.5">
                <Calendar className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                <div>
                  <p className="text-xs text-muted-foreground">Scheduled Time</p>
                  <p className="text-xs font-semibold text-foreground mt-0.5">{formatDateTime(exam.startTime)}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <Clock className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                <div>
                  <p className="text-xs text-muted-foreground">Duration</p>
                  <p className="text-xs font-semibold text-foreground mt-0.5">{formatDuration(exam.duration)}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <MapPin className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                <div>
                  <p className="text-xs text-muted-foreground">Venue</p>
                  <p className="text-xs font-semibold text-foreground mt-0.5">{exam.venue}</p>
                </div>
              </div>
            </div>
          </Card>

          {/* Marks card */}
          <Card className="p-5 space-y-4">
            <h3 className="font-display font-semibold text-foreground text-sm border-b pb-2">Marks & Scheme</h3>
            <div className="grid grid-cols-2 gap-4">
              <div className="p-3 rounded-lg bg-muted/40 text-center">
                <p className="font-bold text-foreground text-lg">{exam.totalMarks}</p>
                <p className="text-xs text-muted-foreground mt-0.5">Total Marks</p>
              </div>
              <div className="p-3 rounded-lg bg-muted/40 text-center">
                <p className="font-bold text-foreground text-lg">{exam.passingMarks}</p>
                <p className="text-xs text-muted-foreground mt-0.5">Passing Marks</p>
              </div>
            </div>
          </Card>

          {/* Action Button */}
          {role === 'student' ? (
            <div className="space-y-2">
              {exam.accessMessage && (
                <div className="p-3 rounded-lg bg-muted text-xs text-muted-foreground border">
                  {exam.accessMessage}
                </div>
              )}
              {exam.canStart ? (
                <Button
                  variant="primary"
                  className="w-full justify-center"
                  onClick={() => navigate(ROUTES.STUDENT.JOIN_EXAM.replace(':examId', exam.id))}
                >
                  {exam.scheduleState === 'IN_PROGRESS' ? 'Resume Test' : 'Start Test (System Check)'}
                </Button>
              ) : exam.scheduleState === 'COMPLETED' ? (
                <Button
                  variant="outline"
                  className="w-full justify-center"
                  onClick={() => navigate(ROUTES.STUDENT.RESULTS)}
                >
                  View Result
                </Button>
              ) : (
                <Button
                  variant="outline"
                  className="w-full justify-center opacity-60"
                  disabled
                >
                  {exam.scheduleState === 'UPCOMING' ? 'Exam Not Available Yet' :
                   exam.scheduleState === 'EXPIRED' ? 'Exam Expired' :
                   exam.scheduleState === 'CANCELLED' ? 'Exam Cancelled' : 'Access Restricted'}
                </Button>
              )}
            </div>
          ) : (
            <div className="space-y-2">
              {isLive ? (
                <Button
                  variant="primary"
                  className="w-full justify-center"
                  onClick={() => navigate(ROUTES.FACULTY.MONITORING)}
                >
                  Monitor Live Exam
                </Button>
              ) : (
                <Button
                  variant="outline"
                  className="w-full justify-center"
                  onClick={() => navigate(ROUTES.FACULTY.MONITORING)}
                >
                  Live Monitoring Desk
                </Button>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Edit Modal */}
      {editModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-card border border-border rounded-xl shadow-xl max-w-lg w-full p-6 space-y-4">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="font-display font-bold text-foreground text-lg">Edit Exam</h3>
              <button onClick={() => setEditModalOpen(false)} className="text-muted-foreground hover:text-foreground">
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleUpdate} className="space-y-4">
              <Input
                label="Exam Title"
                value={editForm.examTitle}
                onChange={(e) => setEditForm((prev) => ({ ...prev, examTitle: e.target.value }))}
                required
              />
              <Input
                label="Venue"
                value={editForm.venue}
                onChange={(e) => setEditForm((prev) => ({ ...prev, venue: e.target.value }))}
                required
              />
              <div className="grid grid-cols-2 gap-3">
                <Input
                  label="Total Marks"
                  type="number"
                  value={editForm.totalMarks}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, totalMarks: parseInt(e.target.value) || 0 }))}
                  required
                />
                <Input
                  label="Passing Marks"
                  type="number"
                  value={editForm.passingMarks}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, passingMarks: parseInt(e.target.value) || 0 }))}
                  required
                />
              </div>
              <Select
                label="Status"
                options={[
                  { value: 'PUBLISHED', label: 'Published' },
                  { value: 'DRAFT', label: 'Draft' },
                  { value: 'COMPLETED', label: 'Completed' },
                  { value: 'CANCELLED', label: 'Cancelled' },
                ]}
                value={editForm.status}
                onChange={(e) => setEditForm((prev) => ({ ...prev, status: e.target.value }))}
              />
              <Textarea
                label="Description"
                value={editForm.description}
                onChange={(e) => setEditForm((prev) => ({ ...prev, description: e.target.value }))}
              />

              <div className="flex gap-3 justify-end pt-2">
                <Button type="button" variant="outline" onClick={() => setEditModalOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" isLoading={submitting}>
                  Save Changes
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={handleDelete}
        title="Delete Examination"
        description={`Are you sure you want to delete "${exam.title}" (${exam.code})? This will perform a soft delete on this exam in PostgreSQL.`}
        confirmLabel="Delete Exam"
        variant="danger"
        isLoading={submitting}
      />
    </div>
  );
}
