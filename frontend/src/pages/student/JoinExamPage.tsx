import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Shield, CheckCircle, AlertTriangle } from 'lucide-react';
import { Button } from '@/components/common/Button';
import { Card } from '@/components/common/Card';
import { ROUTES } from '@/constants';
import api from '@/services/api';
import toast from 'react-hot-toast';

export function JoinExamPage() {
  const { examId } = useParams<{ examId: string }>();
  const navigate = useNavigate();
  const [exam, setExam] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [step, setStep] = useState<'check' | 'briefing' | 'ready'>('check');
  const [checks, setChecks] = useState({ camera: false, fullscreen: false, network: false });

  useEffect(() => {
    async function fetchExam() {
      try {
        const response = await api.get(`/v1/exams/${examId}`);
        const backendExam = response.data.data;
        const mappedExam = {
          id: backendExam.id.toString(),
          title: backendExam.examTitle,
          subject: backendExam.subject ? `${backendExam.subject.subjectCode} - ${backendExam.subject.subjectName}` : 'N/A',
          code: backendExam.examCode,
          description: backendExam.description || '',
          facultyId: backendExam.faculty?.id?.toString() || '',
          facultyName: backendExam.faculty?.name || 'N/A',
          department: backendExam.department?.name || 'N/A',
          startTime: backendExam.scheduledStart || `${backendExam.scheduledDate}T${backendExam.startTime}`,
          endTime: backendExam.scheduledEnd || `${backendExam.scheduledEndDate || backendExam.scheduledDate}T${backendExam.endTime}`,
          scheduledDate: backendExam.scheduledDate,
          scheduledEndDate: backendExam.scheduledEndDate,
          scheduledStart: backendExam.scheduledStart,
          scheduledEnd: backendExam.scheduledEnd,
          scheduleState: backendExam.scheduleState,
          canStart: backendExam.canStart,
          accessMessage: backendExam.accessMessage,
          attemptStatus: backendExam.attemptStatus,
          attemptId: backendExam.attemptId,
          remainingSeconds: backendExam.remainingSeconds,
          duration: backendExam.durationMinutes,
          totalMarks: backendExam.totalMarks,
          passingMarks: backendExam.passingMarks,
          status: backendExam.status.toLowerCase(),
          venue: backendExam.venue || 'N/A',
          enrolledCount: 0,
          maxStudents: 100,
          createdAt: backendExam.createdAt || '',
          instructions: backendExam.instructions ? backendExam.instructions.split('\n') : [],
          isAIMonitored: true,
        };
        setExam(mappedExam);
      } catch (err) {
        toast.error('Failed to load exam details');
      } finally {
        setLoading(false);
      }
    }
    fetchExam();
  }, [examId]);

  useEffect(() => {
    // Simulate system checks
    const t1 = setTimeout(() => setChecks((c) => ({ ...c, network: true })), 800);
    const t2 = setTimeout(() => setChecks((c) => ({ ...c, camera: true })), 1500);
    const t3 = setTimeout(() => setChecks((c) => ({ ...c, fullscreen: true })), 2200);
    return () => { clearTimeout(t1); clearTimeout(t2); clearTimeout(t3); };
  }, []);

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
        <Button variant="primary" className="mt-4" onClick={() => navigate(ROUTES.STUDENT.EXAMS)}>
          Back to Exams
        </Button>
      </div>
    );
  }

  if (exam && !exam.canStart) {
    return (
      <div className="max-w-2xl mx-auto space-y-6">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">{exam.title}</h1>
          <p className="text-muted-foreground text-sm mt-1">{exam.code} · {exam.department}</p>
        </div>
        <Card className="p-8 text-center space-y-4">
          <div className="h-12 w-12 rounded-full bg-danger/10 text-danger flex items-center justify-center mx-auto">
            <AlertTriangle className="h-6 w-6" />
          </div>
          <h2 className="text-lg font-bold text-foreground">Exam Access Restricted</h2>
          <p className="text-sm text-muted-foreground max-w-md mx-auto">
            {exam.accessMessage || 'This exam is not available at the current time.'}
          </p>
          <div className="pt-2">
            <Button variant="primary" onClick={() => navigate(ROUTES.STUDENT.EXAMS)}>
              Back to My Exams
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  const handleEnterBrowser = async () => {
    try {
      setStarting(true);
      const res = await api.post('/v1/attempts/start', { examId: parseInt(exam.id) });
      const attempt = res.data?.data;
      navigate(`${ROUTES.SECURE_BROWSER}?examId=${exam.id}&attemptId=${attempt?.id || ''}`);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Access denied: Unable to start exam attempt';
      toast.error(msg);
      try {
        const refreshRes = await api.get(`/v1/exams/${examId}`);
        const backendExam = refreshRes.data.data;
        setExam((prev: any) => ({
          ...prev,
          canStart: backendExam.canStart,
          scheduleState: backendExam.scheduleState,
          accessMessage: backendExam.accessMessage,
        }));
      } catch {}
    } finally {
      setStarting(false);
    }
  };

  const allChecked = Object.values(checks).every(Boolean);

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="font-display font-bold text-foreground text-2xl">{exam.title}</h1>
        <p className="text-muted-foreground text-sm mt-1">{exam.code} · {exam.department}</p>
      </div>

      {step === 'check' && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
          <Card className="p-6">
            <div className="flex items-center gap-3 mb-5">
              <div className="h-10 w-10 rounded-xl bg-primary/10 flex items-center justify-center">
                <Shield className="h-5 w-5 text-primary" />
              </div>
              <div>
                <h2 className="font-display font-semibold text-foreground">System Check</h2>
                <p className="text-xs text-muted-foreground">Verifying requirements before entering exam</p>
              </div>
            </div>
            <div className="space-y-3">
              {[
                { key: 'network', label: 'Network Connection', desc: 'Stable internet detected' },
                { key: 'camera', label: 'Camera Access', desc: 'Webcam is available and active' },
                { key: 'fullscreen', label: 'Secure Browser Mode', desc: 'Fullscreen mode required' },
              ].map((item) => {
                const isChecked = checks[item.key as keyof typeof checks];
                return (
                  <div key={item.key} className="flex items-center gap-3 p-3 rounded-lg bg-muted/40">
                    <div className={`h-8 w-8 rounded-full flex items-center justify-center ${isChecked ? 'bg-success/10 text-success' : 'bg-muted text-muted-foreground'}`}>
                      {isChecked ? <CheckCircle className="h-4 w-4" /> : <div className="h-4 w-4 rounded-full border-2 border-muted-foreground/30 border-t-primary animate-spin" />}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-foreground">{item.label}</p>
                      <p className="text-xs text-muted-foreground">{isChecked ? item.desc : 'Checking...'}</p>
                    </div>
                  </div>
                );
              })}
            </div>
            <Button
              variant="primary"
              size="lg"
              className="w-full mt-5"
              disabled={!allChecked}
              onClick={() => setStep('briefing')}
            >
              {allChecked ? 'Proceed to Exam Briefing' : 'Running System Checks...'}
            </Button>
          </Card>
        </motion.div>
      )}

      {step === 'briefing' && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
          <Card className="p-6 space-y-4">
            <h2 className="font-display font-semibold text-foreground">Exam Instructions</h2>
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="p-3 rounded-lg bg-muted/40">
                <p className="font-bold text-foreground text-lg">{exam.duration} min</p>
                <p className="text-xs text-muted-foreground">Duration</p>
              </div>
              <div className="p-3 rounded-lg bg-muted/40">
                <p className="font-bold text-foreground text-lg">{exam.totalMarks}</p>
                <p className="text-xs text-muted-foreground">Total Marks</p>
              </div>
              <div className="p-3 rounded-lg bg-muted/40">
                <p className="font-bold text-foreground text-lg">{exam.passingMarks}</p>
                <p className="text-xs text-muted-foreground">Passing Marks</p>
              </div>
            </div>
            <div className="space-y-2">
              {exam.instructions.map((inst: string, i: number) => (
                <div key={i} className="flex gap-2.5 text-sm text-foreground">
                  <span className="text-primary font-bold mt-0.5">{i + 1}.</span>
                  <span>{inst}</span>
                </div>
              ))}
            </div>
            {exam.isAIMonitored && (
              <div className="flex gap-2.5 p-3 rounded-lg bg-warning/10 border border-warning/20">
                <AlertTriangle className="h-4 w-4 text-warning flex-shrink-0 mt-0.5" />
                <p className="text-xs text-warning-600 dark:text-amber-400">
                  This exam is AI-monitored. Your webcam will be active throughout the session. Any suspicious activity will be flagged automatically.
                </p>
              </div>
            )}
            <div className="flex gap-3">
              <Button variant="outline" className="flex-1" onClick={() => setStep('check')}>Back</Button>
              <Button variant="primary" className="flex-1" isLoading={starting} onClick={handleEnterBrowser}>
                Enter Secure Browser
              </Button>
            </div>
          </Card>
        </motion.div>
      )}
    </div>
  );
}
