import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { motion } from 'framer-motion';
import { BookOpen, Plus, Trash2 } from 'lucide-react';
import { Input, Textarea, Select } from '@/components/forms/FormFields';
import { Button } from '@/components/common/Button';
import { Card, CardHeader } from '@/components/common/Card';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants';
import toast from 'react-hot-toast';

const examSchema = z.object({
  title: z.string().min(5, 'Title must be at least 5 characters'),
  subject: z.string().min(2, 'Subject code is required'),
  department: z.string().min(1, 'Department is required'),
  duration: z.string().min(1, 'Duration is required'),
  totalMarks: z.string().min(1, 'Total marks required'),
  passingMarks: z.string().min(1, 'Passing marks required'),
  venue: z.string().min(3, 'Venue is required'),
  startDate: z.string().min(1, 'Start date is required'),
  startTime: z.string().min(1, 'Start time is required'),
  maxStudents: z.string().min(1, 'Max students required'),
  description: z.string().optional(),
  isAIMonitored: z.boolean().optional(),
});

type ExamFormData = z.infer<typeof examSchema>;

const deptOptions = [
  { value: 'cs', label: 'Computer Science' },
  { value: 'ec', label: 'Electronics & Communication' },
  { value: 'me', label: 'Mechanical Engineering' },
  { value: 'ce', label: 'Civil Engineering' },
];

export function CreateExamPage() {
  const navigate = useNavigate();
  const [instructions, setInstructions] = useState<string[]>(['No electronic devices allowed.']);
  const [newInstruction, setNewInstruction] = useState('');

  const { register, handleSubmit, formState: { errors, isSubmitting }, watch } = useForm<ExamFormData>({
    resolver: zodResolver(examSchema),
    defaultValues: { isAIMonitored: true },
  });

  const addInstruction = () => {
    if (newInstruction.trim()) {
      setInstructions((prev) => [...prev, newInstruction.trim()]);
      setNewInstruction('');
    }
  };

  const removeInstruction = (i: number) => {
    setInstructions((prev) => prev.filter((_, idx) => idx !== i));
  };

  const onSubmit = async () => {
    await new Promise((r) => setTimeout(r, 1000));
    toast.success('Exam created successfully!');
    navigate(ROUTES.FACULTY.EXAMS);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="font-display font-bold text-foreground text-2xl">Create Exam</h1>
        <p className="text-muted-foreground text-sm mt-1">Configure and schedule a new examination</p>
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
            <Input
              label="Subject Code"
              placeholder="CS301"
              error={errors.subject?.message}
              required
              {...register('subject')}
            />
            <Select
              label="Department"
              options={deptOptions}
              placeholder="Select department"
              error={errors.department?.message}
              required
              {...register('department')}
            />
            <Textarea
              label="Description"
              placeholder="Brief description of the exam..."
              {...register('description')}
              wrapperClassName="sm:col-span-2"
            />
          </div>
        </Card>

        {/* Schedule */}
        <Card>
          <CardHeader title="Schedule & Venue" icon={<BookOpen className="h-4 w-4" />} />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input label="Start Date" type="date" required {...register('startDate')} error={errors.startDate?.message} />
            <Input label="Start Time" type="time" required {...register('startTime')} error={errors.startTime?.message} />
            <Input label="Duration (minutes)" type="number" placeholder="120" required {...register('duration')} error={errors.duration?.message} />
            <Input label="Venue" placeholder="Hall A - Room 101" required {...register('venue')} error={errors.venue?.message} />
          </div>
        </Card>

        {/* Marking */}
        <Card>
          <CardHeader title="Marking Scheme" icon={<BookOpen className="h-4 w-4" />} />
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Input label="Total Marks" type="number" placeholder="100" required {...register('totalMarks')} error={errors.totalMarks?.message} />
            <Input label="Passing Marks" type="number" placeholder="40" required {...register('passingMarks')} error={errors.passingMarks?.message} />
            <Input label="Max Students" type="number" placeholder="50" required {...register('maxStudents')} error={errors.maxStudents?.message} />
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
            <div>
              <p className="font-semibold text-foreground text-sm">Enable AI Monitoring</p>
              <p className="text-xs text-muted-foreground mt-0.5">Real-time face detection and behavior analysis</p>
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
            Create Exam
          </Button>
        </div>
      </form>
    </div>
  );
}
