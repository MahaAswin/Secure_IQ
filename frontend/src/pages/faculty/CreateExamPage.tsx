import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { motion } from 'framer-motion';
import { BookOpen, Plus, Trash2, Calendar, Clock, Award, ShieldAlert } from 'lucide-react';
import { Input, Textarea, Select } from '@/components/forms/FormFields';
import { Button } from '@/components/common/Button';
import { Card, CardHeader } from '@/components/common/Card';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants';
import toast from 'react-hot-toast';
import api from '@/services/api';

const examSchema = z.object({
  title: z.string().min(5, 'Title must be at least 5 characters'),
  subjectId: z.string().min(1, 'Subject is required'),
  examType: z.string().min(1, 'Exam type is required'),
  durationMinutes: z.string().refine((val) => parseInt(val) > 0, 'Duration must be positive'),
  totalMarks: z.string().refine((val) => parseInt(val) > 0, 'Total marks must be positive'),
  passingMarks: z.string().refine((val) => parseInt(val) >= 0, 'Passing marks cannot be negative'),
  venue: z.string().min(3, 'Venue is required'),
  startDate: z.string().min(1, 'Start date is required'),
  startTime: z.string().min(1, 'Start time is required'),
  endDate: z.string().min(1, 'End date is required'),
  endTime: z.string().min(1, 'End time is required'),
  status: z.enum(['PUBLISHED', 'DRAFT']),
  description: z.string().optional(),
  isAIMonitored: z.boolean().optional(),
}).refine((data) => parseInt(data.passingMarks) <= parseInt(data.totalMarks), {
  message: 'Passing marks cannot exceed total marks',
  path: ['passingMarks'],
}).refine((data) => {
  if (!data.startDate || !data.startTime || !data.endDate || !data.endTime) return true;
  const start = new Date(`${data.startDate}T${data.startTime}`);
  const end = new Date(`${data.endDate}T${data.endTime}`);
  return end.getTime() > start.getTime();
}, {
  message: 'Scheduled end time must be strictly after scheduled start time',
  path: ['endTime'],
}).refine((data) => {
  if (!data.startDate || !data.startTime || !data.endDate || !data.endTime || !data.durationMinutes) return true;
  const start = new Date(`${data.startDate}T${data.startTime}`);
  const end = new Date(`${data.endDate}T${data.endTime}`);
  const windowMinutes = (end.getTime() - start.getTime()) / (1000 * 60);
  return parseInt(data.durationMinutes) <= windowMinutes;
}, {
  message: 'Exam duration cannot exceed the scheduled window',
  path: ['durationMinutes'],
});

type ExamFormData = z.infer<typeof examSchema>;

export function CreateExamPage() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState<any>(null);
  const [subjects, setSubjects] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [instructions, setInstructions] = useState<string[]>([
    'No electronic devices allowed in the exam hall.',
    'Rough work must be done on the provided sheets only.',
    'All questions are compulsory.',
  ]);
  const [newInstruction, setNewInstruction] = useState('');

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ExamFormData>({
    resolver: zodResolver(examSchema),
    defaultValues: {
      isAIMonitored: true,
      status: 'PUBLISHED',
      durationMinutes: '120',
      totalMarks: '100',
      passingMarks: '40',
      startDate: new Date().toISOString().split('T')[0],
      startTime: '10:00',
      endDate: new Date().toISOString().split('T')[0],
      endTime: '12:00',
    },
  });

  useEffect(() => {
    async function loadPrerequisites() {
      try {
        setLoading(true);
        const [profileRes, subjectsRes] = await Promise.allSettled([
          api.get('/v1/faculties/profile'),
          api.get('/v1/subjects', { params: { size: 100 } }),
        ]);

        let prof: any = null;
        if (profileRes.status === 'fulfilled') {
          prof = profileRes.value.data?.data;
          setProfile(prof);
        }

        let allSubjs: any[] = [];
        if (subjectsRes.status === 'fulfilled') {
          allSubjs = subjectsRes.value.data?.data?.content || [];
        }

        // Combine profile subjects and department subjects
        const combinedMap = new Map<number, any>();
        if (prof?.subjects) {
          prof.subjects.forEach((s: any) => combinedMap.set(s.id, s));
        }
        allSubjs.forEach((s: any) => {
          if (!combinedMap.has(s.id)) {
            // Include subjects for faculty's department if known
            if (!prof?.department?.id || s.department?.id === prof.department.id) {
              combinedMap.set(s.id, s);
            }
          }
        });

        // If still empty, use all subjects
        if (combinedMap.size === 0 && allSubjs.length > 0) {
          allSubjs.forEach((s: any) => combinedMap.set(s.id, s));
        }

        setSubjects(Array.from(combinedMap.values()));
      } catch (err) {
        toast.error('Failed to load profile or subjects');
      } finally {
        setLoading(false);
      }
    }
    loadPrerequisites();
  }, []);

  const addInstruction = () => {
    if (newInstruction.trim()) {
      setInstructions((prev) => [...prev, newInstruction.trim()]);
      setNewInstruction('');
    }
  };

  const removeInstruction = (i: number) => {
    setInstructions((prev) => prev.filter((_, idx) => idx !== i));
  };

  const onSubmit = async (data: ExamFormData) => {
    try {
      if (!profile) {
        toast.error('Faculty profile could not be loaded. Please ensure you are logged in as a faculty member.');
        return;
      }

      const selectedSubject = subjects.find((s) => s.id.toString() === data.subjectId);
      if (!selectedSubject) {
        toast.error('Selected subject not found');
        return;
      }

      // Check date is not in the past
      const todayStr = new Date().toISOString().split('T')[0];
      if (data.startDate < todayStr) {
        toast.error('Scheduled date cannot be in the past');
        return;
      }

      const startDateTime = new Date(`${data.startDate}T${data.startTime}`);
      const endDateTime = new Date(`${data.endDate}T${data.endTime}`);
      if (endDateTime.getTime() <= startDateTime.getTime()) {
        toast.error('Scheduled end time must be after scheduled start time');
        return;
      }

      const windowMinutes = (endDateTime.getTime() - startDateTime.getTime()) / (1000 * 60);
      const duration = parseInt(data.durationMinutes);
      if (duration > windowMinutes) {
        toast.error(`Exam duration (${duration} mins) cannot exceed the scheduled window (${Math.round(windowMinutes)} mins)`);
        return;
      }

      const startTimeStr = data.startTime.length === 5 ? `${data.startTime}:00` : data.startTime;
      const endTimeStr = data.endTime.length === 5 ? `${data.endTime}:00` : data.endTime;

      // Unique Exam Code
      const rand = Math.floor(100 + Math.random() * 900);
      const subjectCode = selectedSubject.subjectCode || 'EXAM';
      const dateCode = data.startDate.replace(/-/g, '');
      const examCode = `${subjectCode}-${dateCode}-${rand}`;

      const departmentId = profile.department?.id || selectedSubject.department?.id;
      if (!departmentId) {
        toast.error('Department ID is missing. Please contact an administrator.');
        return;
      }

      const payload = {
        examCode,
        examTitle: data.title,
        description: data.description || '',
        subjectId: parseInt(data.subjectId),
        facultyId: profile.id,
        departmentId: departmentId,
        semester: selectedSubject.semester || 1,
        examType: data.examType,
        totalMarks: parseInt(data.totalMarks),
        durationMinutes: duration,
        passingMarks: parseInt(data.passingMarks),
        scheduledDate: data.startDate,
        scheduledEndDate: data.endDate,
        startTime: startTimeStr,
        endTime: endTimeStr,
        venue: data.venue,
        instructions: instructions.join('\n'),
        status: data.status,
      };

      const examRes = await api.post('/v1/exams', payload);
      const createdExam = examRes.data.data;

      // Automatically create an exam session so the exam has a runnable session
      try {
        const sessionPayload = {
          sessionCode: `${examCode}-S1`,
          sessionName: `${data.title} - Session 1`,
          examId: createdExam.id,
          facultyId: profile.id,
          startDateTime: `${data.startDate}T${startTimeStr}`,
          endDateTime: `${data.endDate}T${endTimeStr}`,
          venue: data.venue,
          instructions: instructions.join('\n'),
          status: 'SCHEDULED',
        };
        await api.post('/v1/exam-sessions', sessionPayload);
      } catch {
        // Exam was created; session creation failure is non-fatal
      }

      toast.success('Exam created and scheduled successfully!');
      navigate(ROUTES.FACULTY.EXAMS);
    } catch (err: any) {
      const errMsg = err.response?.data?.message || err.message || 'Failed to create exam';
      toast.error(errMsg);
    }
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  const subjectOptions = subjects.map((sub: any) => ({
    value: sub.id.toString(),
    label: `${sub.subjectCode} - ${sub.subjectName}`,
  }));

  const examTypeOptions = [
    { value: 'MID_SEMESTER', label: 'Mid-Semester' },
    { value: 'END_SEMESTER', label: 'End-Semester' },
    { value: 'INTERNAL', label: 'Internal Assessment' },
    { value: 'MODEL', label: 'Model Exam' },
    { value: 'QUIZ', label: 'Quiz' },
    { value: 'LAB', label: 'Lab Exam' },
    { value: 'PRACTICAL', label: 'Practical Exam' },
  ];

  const statusOptions = [
    { value: 'PUBLISHED', label: 'Published (Scheduled & Available)' },
    { value: 'DRAFT', label: 'Draft (Visible to Faculty Only)' },
  ];

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="font-display font-bold text-foreground text-2xl">Create Exam</h1>
        <p className="text-muted-foreground text-sm mt-1">Configure, schedule, and persist a new examination</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {/* Basic info */}
        <Card>
          <CardHeader title="Basic Information" icon={<BookOpen className="h-4 w-4" />} />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Exam Title"
              placeholder="Data Structures Mid-Semester"
              error={errors.title?.message}
              required
              {...register('title')}
              className="sm:col-span-2"
              wrapperClassName="sm:col-span-2"
            />
            <Select
              label="Subject"
              options={subjectOptions}
              placeholder={subjectOptions.length === 0 ? 'No subjects found' : 'Select subject'}
              error={errors.subjectId?.message}
              required
              {...register('subjectId')}
            />
            <Input
              label="Department"
              value={profile?.department?.name || 'Computer Science'}
              disabled
            />
            <Select
              label="Exam Type"
              options={examTypeOptions}
              placeholder="Select exam type"
              error={errors.examType?.message}
              required
              {...register('examType')}
            />
            <Select
              label="Exam Status"
              options={statusOptions}
              error={errors.status?.message}
              required
              {...register('status')}
            />
            <Textarea
              label="Description"
              placeholder="Brief description of the exam syllabus and topics covered..."
              {...register('description')}
              wrapperClassName="sm:col-span-2"
            />
          </div>
        </Card>

        {/* Schedule */}
        <Card>
          <CardHeader title="Schedule & Venue" icon={<Calendar className="h-4 w-4" />} />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input label="Scheduled Start Date" type="date" required {...register('startDate')} error={errors.startDate?.message} />
            <Input label="Start Time" type="time" required {...register('startTime')} error={errors.startTime?.message} />
            <Input label="Scheduled End Date" type="date" required {...register('endDate')} error={errors.endDate?.message} />
            <Input label="End Time" type="time" required {...register('endTime')} error={errors.endTime?.message} />
            <Input label="Duration (minutes)" type="number" placeholder="120" required {...register('durationMinutes')} error={errors.durationMinutes?.message} />
            <Input label="Venue" placeholder="Hall A - Room 101" required {...register('venue')} error={errors.venue?.message} />
          </div>
        </Card>

        {/* Marking */}
        <Card>
          <CardHeader title="Marking Scheme" icon={<Award className="h-4 w-4" />} />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input label="Total Marks" type="number" placeholder="100" required {...register('totalMarks')} error={errors.totalMarks?.message} />
            <Input label="Passing Marks" type="number" placeholder="40" required {...register('passingMarks')} error={errors.passingMarks?.message} />
          </div>
        </Card>

        {/* Instructions */}
        <Card>
          <CardHeader title="Instructions" icon={<BookOpen className="h-4 w-4" />} />
          <div className="space-y-2 mb-3">
            {instructions.map((inst, i) => (
              <motion.div
                key={i}
                initial={{ opacity: 0, x: -8 }}
                animate={{ opacity: 1, x: 0 }}
                className="flex items-center gap-2 p-2.5 rounded-lg bg-muted/40"
              >
                <span className="text-xs font-bold text-primary w-5">{i + 1}.</span>
                <span className="flex-1 text-sm text-foreground">{inst}</span>
                <button type="button" onClick={() => removeInstruction(i)} className="text-muted-foreground hover:text-danger transition-colors">
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </motion.div>
            ))}
          </div>
          <div className="flex gap-2">
            <input
              type="text"
              value={newInstruction}
              onChange={(e) => setNewInstruction(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addInstruction())}
              placeholder="Add an instruction..."
              className="flex-1 h-9 rounded-lg border border-border bg-card px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            />
            <Button type="button" variant="outline" size="sm" onClick={addInstruction} leftIcon={<Plus className="h-3.5 w-3.5" />}>
              Add
            </Button>
          </div>
        </Card>

        {/* AI Monitoring */}
        <Card>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="h-9 w-9 rounded-lg bg-secondary/10 flex items-center justify-center text-secondary-600">
                <ShieldAlert className="h-5 w-5" />
              </div>
              <div>
                <p className="font-semibold text-foreground text-sm">Enable AI Proctoring & Monitoring</p>
                <p className="text-xs text-muted-foreground mt-0.5">Real-time face detection, tab switch detection, and behavior analysis</p>
              </div>
            </div>
            <label className="relative inline-flex cursor-pointer items-center">
              <input type="checkbox" className="sr-only peer" defaultChecked {...register('isAIMonitored')} />
              <div className="h-6 w-11 rounded-full bg-muted peer-checked:bg-primary transition-colors after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:bg-white after:shadow after:transition-all peer-checked:after:translate-x-5" />
            </label>
          </div>
        </Card>

        <div className="flex gap-3">
          <Button type="button" variant="outline" className="flex-1" onClick={() => navigate(ROUTES.FACULTY.EXAMS)}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" className="flex-1" isLoading={isSubmitting}>
            Create & Persist Exam
          </Button>
        </div>
      </form>
    </div>
  );
}
