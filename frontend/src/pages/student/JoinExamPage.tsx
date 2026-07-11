import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Shield, Camera, Clock, AlertTriangle, CheckCircle, Lock } from 'lucide-react';
import { Button } from '@/components/common/Button';
import { Card } from '@/components/common/Card';
import { mockExams } from '@/services/mockData';
import { formatSeconds } from '@/utils';
import { ROUTES } from '@/constants';

export function JoinExamPage() {
  const { examId } = useParams<{ examId: string }>();
  const navigate = useNavigate();
  const exam = mockExams.find((e) => e.id === examId) ?? mockExams[0];
  const [step, setStep] = useState<'check' | 'briefing' | 'ready'>('check');
  const [checks, setChecks] = useState({ camera: false, fullscreen: false, network: false });
  const [timeLeft, setTimeLeft] = useState(exam.duration * 60);

  useEffect(() => {
    // Simulate system checks
    const t1 = setTimeout(() => setChecks((c) => ({ ...c, network: true })), 800);
    const t2 = setTimeout(() => setChecks((c) => ({ ...c, camera: true })), 1500);
    const t3 = setTimeout(() => setChecks((c) => ({ ...c, fullscreen: true })), 2200);
    return () => { clearTimeout(t1); clearTimeout(t2); clearTimeout(t3); };
  }, []);

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
              {exam.instructions.map((inst, i) => (
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
              <Button variant="primary" className="flex-1" onClick={() => navigate(ROUTES.SECURE_BROWSER)}>
                Enter Secure Browser
              </Button>
            </div>
          </Card>
        </motion.div>
      )}
    </div>
  );
}
