import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Shield, Camera, Lock, Clock, AlertTriangle, CheckCircle, X, Maximize, RefreshCw, XCircle } from 'lucide-react';
import { Button } from '@/components/common/Button';
import { Badge } from '@/components/common/Badge';
import { useNavigate } from 'react-router-dom';
import { ROUTES, STORAGE_KEYS } from '@/constants';
import { formatSeconds } from '@/utils';
import api from '@/services/api';
import toast from 'react-hot-toast';
import { WebcamMonitor, WebcamMonitorStatus } from '@/components/monitoring/WebcamMonitor';

export function SecureBrowserPage() {
  const navigate = useNavigate();
  const [exam, setExam] = useState<any>(null);
  const [questions, setQuestions] = useState<any[]>([]);
  const [accessDenied, setAccessDenied] = useState<string | null>(null);
  const [attempt, setAttempt] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [timeLeft, setTimeLeft] = useState(7200); // 2 hours default
  const [currentQ, setCurrentQ] = useState(0);
  const [answers, setAnswers] = useState<Record<number, string>>({});
  const [submitOpen, setSubmitOpen] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isFaceCaptured, setIsFaceCaptured] = useState(false);
  const [monitorStatus, setMonitorStatus] = useState<WebcamMonitorStatus>({
    cameraState: 'CAMERA_INITIALIZING',
    detectorState: 'DETECTOR_INITIALIZING',
    facesDetected: 0,
    lastFrameProcessedAt: null,
  });

  useEffect(() => {
    async function loadSecureBrowserData() {
      try {
        const searchParams = new URLSearchParams(window.location.search);
        const examId = searchParams.get('examId');
        const paramAttemptId = searchParams.get('attemptId');
        if (!examId) {
          toast.error('No exam ID specified');
          navigate(ROUTES.STUDENT.DASHBOARD);
          return;
        }

        // 1. Fetch authoritative exam details
        let backendExam: any;
        try {
          const examRes = await api.get(`/v1/exams/${examId}`);
          backendExam = examRes.data.data;
          setExam(backendExam);
        } catch (err: any) {
          const msg = err.response?.data?.message || 'Could not load exam details.';
          setAccessDenied(msg);
          setLoading(false);
          return;
        }

        // Check if student is allowed to start or access exam
        if (backendExam.canStart === false && !paramAttemptId) {
          setAccessDenied(backendExam.accessMessage || 'This exam is not available at the scheduled time.');
          setLoading(false);
          return;
        }

        // 2. Authoritative attempt resolution
        let currentAttempt: any = null;

        if (paramAttemptId) {
          try {
            const attRes = await api.get(`/v1/attempts/${paramAttemptId}`);
            currentAttempt = attRes.data.data;
          } catch {
            // Fall back to starting or resuming attempt below
          }
        }

        if (!currentAttempt) {
          try {
            const startRes = await api.post('/v1/attempts/start', {
              examId: backendExam.id,
              resume: true
            });
            currentAttempt = startRes.data.data;
          } catch (err: any) {
            const msg = err.response?.data?.message || 'Access denied by exam schedule rules.';
            setAccessDenied(msg);
            setLoading(false);
            return;
          }
        }

        if (!currentAttempt || currentAttempt.status === 'SUBMITTED' || currentAttempt.status === 'AUTO_SUBMITTED' || currentAttempt.status === 'TERMINATED' || currentAttempt.status === 'EXPIRED') {
          const reason = currentAttempt?.status === 'SUBMITTED' || currentAttempt?.status === 'AUTO_SUBMITTED'
            ? 'This exam has already been submitted.'
            : currentAttempt?.status === 'EXPIRED'
            ? 'Your exam attempt has expired.'
            : currentAttempt?.status === 'TERMINATED'
            ? 'Your exam attempt has been terminated.'
            : 'Access denied: Unable to access exam attempt.';
          setAccessDenied(reason);
          setLoading(false);
          return;
        }

        // Authoritative countdown initialization
        const rem = currentAttempt.remainingSeconds !== undefined && currentAttempt.remainingSeconds !== null
          ? currentAttempt.remainingSeconds
          : (backendExam.durationMinutes * 60);

        if (rem <= 0) {
          setAccessDenied('Your exam attempt deadline has expired.');
          setLoading(false);
          return;
        }

        setTimeLeft(rem);
        setAttempt(currentAttempt);

        // 3. Fetch questions
        const subjectId = backendExam.subject?.id;
        if (subjectId) {
          try {
            const questionsRes = await api.get('/v1/questions', {
              params: { subjectId, size: 100 }
            });
            const fetchedQList = questionsRes.data.data?.content || [];
            if (fetchedQList.length > 0) {
              const mappedQList = fetchedQList.map((q: any, index: number) => ({
                id: index,
                realId: q.id,
                text: q.questionText,
                options: q.options || [],
                correctAnswer: q.correctAnswer,
                marks: q.marks || 5
              }));
              setQuestions(mappedQList);
            } else {
              setQuestions([
                { id: 0, text: 'Which data structure uses LIFO (Last In First Out) ordering?', options: ['Queue', 'Stack', 'Linked List', 'Binary Tree'], correctAnswer: 'Stack', marks: 5 },
                { id: 1, text: 'What is the time complexity of binary search?', options: ['O(n)', 'O(n²)', 'O(log n)', 'O(n log n)'], correctAnswer: 'O(log n)', marks: 5 },
                { id: 2, text: 'Which sorting algorithm has the best average case complexity?', options: ['Bubble Sort', 'Quick Sort', 'Merge Sort', 'Insertion Sort'], correctAnswer: 'Merge Sort', marks: 5 },
                { id: 3, text: 'What does SQL stand for?', options: ['Structured Query Language', 'Simple Query Language', 'Structured Question Language', 'System Query Language'], correctAnswer: 'Structured Query Language', marks: 5 },
                { id: 4, text: 'Which tree traversal visits root first, then left, then right?', options: ['In-order', 'Post-order', 'Pre-order', 'Level-order'], correctAnswer: 'Pre-order', marks: 5 },
              ]);
            }
          } catch {
            setQuestions([
              { id: 0, text: 'Which data structure uses LIFO (Last In First Out) ordering?', options: ['Queue', 'Stack', 'Linked List', 'Binary Tree'], correctAnswer: 'Stack', marks: 5 },
              { id: 1, text: 'What is the time complexity of binary search?', options: ['O(n)', 'O(n²)', 'O(log n)', 'O(n log n)'], correctAnswer: 'O(log n)', marks: 5 },
              { id: 2, text: 'Which sorting algorithm has the best average case complexity?', options: ['Bubble Sort', 'Quick Sort', 'Merge Sort', 'Insertion Sort'], correctAnswer: 'Merge Sort', marks: 5 },
            ]);
          }
        } else {
          setQuestions([
            { id: 0, text: 'Which data structure uses LIFO (Last In First Out) ordering?', options: ['Queue', 'Stack', 'Linked List', 'Binary Tree'], correctAnswer: 'Stack', marks: 5 },
            { id: 1, text: 'What is the time complexity of binary search?', options: ['O(n)', 'O(n²)', 'O(log n)', 'O(n log n)'], correctAnswer: 'O(log n)', marks: 5 },
            { id: 2, text: 'Which sorting algorithm has the best average case complexity?', options: ['Bubble Sort', 'Quick Sort', 'Merge Sort', 'Insertion Sort'], correctAnswer: 'Merge Sort', marks: 5 },
          ]);
        }
      } catch (err) {
        toast.error('Failed to load secure browser session.');
        navigate(ROUTES.STUDENT.DASHBOARD);
      } finally {
        setLoading(false);
      }
    }
    loadSecureBrowserData();
  }, [navigate]);

  const handleAutoSubmit = async () => {
    if (!attempt || isSubmitted) return;
    try {
      let obtainedMarks = 0;
      questions.forEach((q) => {
        const studentAns = answers[q.id];
        if (studentAns === q.correctAnswer) {
          obtainedMarks += q.marks;
        }
      });

      await api.patch('/v1/attempts/submit', {
        attemptId: attempt.id,
        obtainedMarks,
        autoSubmitted: true
      });

      setIsSubmitted(true);
      toast.error('Exam time has expired! Your attempt has been automatically submitted.');
      navigate(ROUTES.STUDENT.RESULTS);
    } catch {
      toast.error('Exam period expired. Redirecting to results.');
      navigate(ROUTES.STUDENT.RESULTS);
    }
  };

  useEffect(() => {
    if (!attempt || isSubmitted || accessDenied) return;
    const timer = setInterval(() => {
      setTimeLeft((t) => {
        if (t <= 1) {
          clearInterval(timer);
          handleAutoSubmit();
          return 0;
        }
        return t - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [attempt, isSubmitted, accessDenied]);

  if (loading) {
    return (
      <div className="fixed inset-0 bg-dark-950 text-white flex items-center justify-center z-50">
        <div className="text-center space-y-4">
          <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin mx-auto" />
          <p className="text-sm text-dark-400">Entering Secure Browser Environment...</p>
        </div>
      </div>
    );
  }

  if (accessDenied) {
    return (
      <div className="fixed inset-0 bg-dark-950 text-white flex items-center justify-center p-6 z-50">
        <div className="max-w-md w-full bg-dark-900 border border-dark-700 rounded-2xl p-8 text-center space-y-5 shadow-2xl">
          <div className="h-14 w-14 rounded-full bg-danger/10 border border-danger/30 flex items-center justify-center mx-auto text-danger">
            <AlertTriangle className="h-7 w-7" />
          </div>
          <div className="space-y-2">
            <h2 className="text-xl font-display font-bold text-white">Exam Access Denied</h2>
            <p className="text-sm text-dark-300 leading-relaxed">{accessDenied}</p>
          </div>
          <Button variant="primary" className="w-full" onClick={() => navigate(ROUTES.STUDENT.EXAMS)}>
            Back to My Exams
          </Button>
        </div>
      </div>
    );
  }

  if (!exam || questions.length === 0 || !attempt) {
    return null;
  }

  const answered = Object.keys(answers).length;
  const progress = (answered / questions.length) * 100;
  const timeWarning = timeLeft < 600;

  const handleSubmit = async () => {
    try {
      let obtainedMarks = 0;
      questions.forEach((q) => {
        const studentAns = answers[q.id];
        if (studentAns === q.correctAnswer) {
          obtainedMarks += q.marks;
        }
      });

      await api.patch('/v1/attempts/submit', {
        attemptId: attempt.id,
        obtainedMarks,
        autoSubmitted: false
      });

      setIsSubmitted(true);
      toast.success('Exam submitted successfully!');
      navigate(ROUTES.STUDENT.RESULTS);
    } catch (err) {
      toast.error('Failed to submit exam attempt');
    }
  };

  return (
    <div className="fixed inset-0 bg-dark-950 text-white flex flex-col overflow-hidden z-50">
      {/* Top bar */}
      <div className="flex items-center justify-between px-6 py-3 border-b border-dark-800 bg-dark-900 flex-shrink-0">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <div className="h-7 w-7 rounded-lg bg-gradient-primary flex items-center justify-center text-white font-bold text-sm">S</div>
            <span className="font-display font-semibold text-white">SecureIQ</span>
          </div>
          <div className="h-4 w-px bg-dark-700" />
          <div>
            <p className="text-sm font-medium text-white">{exam.examTitle}</p>
            <p className="text-xs text-dark-400">{exam.examCode}</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          {/* Real Dynamic Camera & AI Proctor Status */}
          {monitorStatus.cameraState === 'CAMERA_ACTIVE' && monitorStatus.detectorState === 'MONITORING_ACTIVE' && (
            <div className="flex items-center gap-1.5 text-xs text-green-400 font-medium">
              <span className="h-2 w-2 rounded-full bg-green-400 animate-pulse" />
              <span>AI Proctor Active</span>
            </div>
          )}
          {monitorStatus.cameraState === 'CAMERA_ACTIVE' && monitorStatus.detectorState !== 'MONITORING_ACTIVE' && (
            <div className="flex items-center gap-1.5 text-xs text-yellow-400 font-medium">
              <Camera className="h-3.5 w-3.5" />
              <span>Camera Active (AI Syncing)</span>
            </div>
          )}
          {monitorStatus.cameraState === 'CAMERA_INITIALIZING' && (
            <div className="flex items-center gap-1.5 text-xs text-blue-400 font-medium">
              <RefreshCw className="h-3.5 w-3.5 animate-spin" />
              <span>Camera Initializing...</span>
            </div>
          )}
          {(monitorStatus.cameraState === 'PERMISSION_DENIED' || monitorStatus.cameraState === 'CAMERA_ERROR') && (
            <div className="flex items-center gap-1.5 text-xs text-danger font-medium">
              <AlertTriangle className="h-3.5 w-3.5" />
              <span>Camera Blocked</span>
            </div>
          )}
          {monitorStatus.cameraState === 'DEVICE_UNAVAILABLE' && (
            <div className="flex items-center gap-1.5 text-xs text-warning font-medium">
              <AlertTriangle className="h-3.5 w-3.5" />
              <span>No Camera Found</span>
            </div>
          )}
          {monitorStatus.cameraState === 'CAMERA_STOPPED' && (
            <div className="flex items-center gap-1.5 text-xs text-dark-400 font-medium">
              <Camera className="h-3.5 w-3.5 text-dark-500" />
              <span>Camera Stopped</span>
            </div>
          )}
          <div className="h-4 w-px bg-dark-700" />

          {/* Timer */}
          <div className={`flex items-center gap-2 px-3 py-1.5 rounded-lg ${timeWarning ? 'bg-danger/20 text-danger-400' : 'bg-dark-800 text-white'}`}>
            <Clock className={`h-3.5 w-3.5 ${timeWarning ? 'animate-pulse' : ''}`} />
            <span className="font-mono font-bold text-base">{formatSeconds(timeLeft)}</span>
          </div>
          <div className="h-4 w-px bg-dark-700" />

          {/* Secure mode */}
          <div className="flex items-center gap-1.5 text-xs text-blue-400">
            <Lock className="h-3.5 w-3.5" />
            <span>Secure Mode</span>
          </div>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Question panel */}
        <div className="w-64 border-r border-dark-800 bg-dark-900 flex flex-col flex-shrink-0">
          <div className="p-4 border-b border-dark-800">
            <p className="text-xs font-semibold text-dark-400 uppercase tracking-wide mb-2">Progress</p>
            <div className="flex justify-between text-xs text-dark-300 mb-1.5">
              <span>{answered} answered</span>
              <span>{questions.length - answered} remaining</span>
            </div>
            <div className="h-2 w-full rounded-full bg-dark-700 overflow-hidden">
              <div className="h-full rounded-full bg-primary transition-all duration-500" style={{ width: `${progress}%` }} />
            </div>
          </div>
          <div className="flex-1 overflow-y-auto p-3">
            <div className="grid grid-cols-4 gap-1.5">
              {questions.map((q) => (
                <button
                  key={q.id}
                  onClick={() => isFaceCaptured && setCurrentQ(q.id)}
                  disabled={!isFaceCaptured}
                  className={`h-8 w-full rounded-md text-xs font-semibold transition-colors ${
                    !isFaceCaptured
                      ? 'bg-dark-900 text-dark-600 border border-dark-800 cursor-not-allowed opacity-50'
                      : answers[q.id]
                      ? 'bg-success/20 text-green-400 border border-success/30'
                      : currentQ === q.id
                      ? 'bg-primary text-white'
                      : 'bg-dark-800 text-dark-400 hover:bg-dark-700 border border-dark-700'
                  }`}
                >
                  {q.id + 1}
                </button>
              ))}
            </div>
          </div>
          <div className="p-3 border-t border-dark-800">
            <Button
              variant="primary"
              className="w-full"
              disabled={!isFaceCaptured}
              onClick={() => setSubmitOpen(true)}
            >
              Submit Exam
            </Button>
          </div>
        </div>

        {/* Main Content: Locked Face Verification Gate vs Questions */}
        {!isFaceCaptured ? (
          <div className="flex-1 flex flex-col items-center justify-center p-8 text-center bg-dark-950">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="max-w-md w-full bg-dark-900 border border-dark-700 rounded-2xl p-8 space-y-6 shadow-2xl"
            >
              <div className="h-16 w-16 rounded-full bg-primary/10 border border-primary/30 flex items-center justify-center mx-auto text-primary">
                <Camera className="h-8 w-8 animate-pulse" />
              </div>

              <div className="space-y-2">
                <h2 className="text-xl font-display font-bold text-white">AI Proctor Face Verification</h2>
                <p className="text-xs text-dark-300 leading-relaxed">
                  Exam questions will display once your camera is active and your face is captured by the AI proctor.
                </p>
              </div>

              {/* Status checklist */}
              <div className="space-y-2.5 text-left bg-dark-950 p-4 rounded-xl border border-dark-800 text-xs">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    {monitorStatus.cameraState === 'CAMERA_ACTIVE' ? (
                      <CheckCircle className="h-4 w-4 text-green-400" />
                    ) : monitorStatus.cameraState === 'PERMISSION_DENIED' ? (
                      <XCircle className="h-4 w-4 text-danger" />
                    ) : (
                      <RefreshCw className="h-4 w-4 text-primary animate-spin" />
                    )}
                    <span className="text-dark-200">Camera Feed</span>
                  </div>
                  <span className={`font-semibold ${monitorStatus.cameraState === 'CAMERA_ACTIVE' ? 'text-green-400' : 'text-dark-400'}`}>
                    {monitorStatus.cameraState === 'CAMERA_ACTIVE' ? 'Connected' : monitorStatus.cameraState === 'PERMISSION_DENIED' ? 'Blocked' : 'Initializing...'}
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    {monitorStatus.facesDetected >= 1 ? (
                      <CheckCircle className="h-4 w-4 text-green-400" />
                    ) : (
                      <RefreshCw className="h-4 w-4 text-yellow-400 animate-spin" />
                    )}
                    <span className="text-dark-200">Face Recognition</span>
                  </div>
                  <span className={`font-semibold ${monitorStatus.facesDetected >= 1 ? 'text-green-400' : 'text-yellow-400'}`}>
                    {monitorStatus.facesDetected >= 1 ? 'Face Captured' : 'Looking for face...'}
                  </span>
                </div>
              </div>

              {/* Instructions and Unlock Action */}
              <div className="space-y-3">
                <p className="text-[11px] text-dark-400 bg-dark-800/60 p-3 rounded-lg border border-dark-700/60 text-left">
                  💡 <strong>Tip:</strong> Please look directly at the webcam preview on the right panel.
                </p>

                {monitorStatus.cameraState === 'CAMERA_ACTIVE' && (
                  <Button
                    variant="primary"
                    className="w-full text-xs"
                    onClick={() => setIsFaceCaptured(true)}
                  >
                    {monitorStatus.facesDetected >= 1 ? 'Face Verified — Start Exam' : 'Unlock Questions'}
                  </Button>
                )}
              </div>
            </motion.div>
          </div>
        ) : (
          <div className="flex-1 overflow-y-auto p-8">
            <motion.div
              key={currentQ}
              initial={{ opacity: 0, x: 16 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.25 }}
              className="max-w-2xl mx-auto"
            >
              <div className="flex items-center gap-3 mb-6">
                <span className="h-8 w-8 rounded-full bg-primary/20 text-primary text-sm font-bold flex items-center justify-center">
                  {currentQ + 1}
                </span>
                <span className="text-xs text-dark-400">{questions[currentQ].marks} marks</span>
              </div>

              <p className="text-white text-lg font-medium mb-6 leading-relaxed">
                {questions[currentQ].text}
              </p>

              <div className="space-y-3">
                {questions[currentQ].options.map((opt: string, i: number) => {
                  const letter = String.fromCharCode(65 + i);
                  const isSelected = answers[currentQ] === opt;
                  return (
                    <motion.button
                      key={opt}
                      whileTap={{ scale: 0.99 }}
                      onClick={() => setAnswers((prev) => ({ ...prev, [currentQ]: opt }))}
                      className={`w-full flex items-center gap-4 p-4 rounded-xl border text-left transition-all duration-200 ${
                        isSelected
                          ? 'border-primary bg-primary/10 text-primary'
                          : 'border-dark-700 bg-dark-800 text-dark-200 hover:border-dark-500'
                      }`}
                    >
                      <span className={`h-8 w-8 rounded-full border-2 flex items-center justify-center text-sm font-bold flex-shrink-0 ${
                        isSelected ? 'border-primary bg-primary text-white' : 'border-dark-600 text-dark-400'
                      }`}>
                        {letter}
                      </span>
                      <span className="text-sm">{opt}</span>
                      {isSelected && <CheckCircle className="h-4 w-4 text-primary ml-auto" />}
                    </motion.button>
                  );
                })}
              </div>

              {/* Navigation */}
              <div className="flex justify-between mt-8">
                <Button
                  variant="outline"
                  className="border-dark-700 text-dark-300 hover:bg-dark-800"
                  onClick={() => setCurrentQ((q) => Math.max(0, q - 1))}
                  disabled={currentQ === 0}
                >
                  Previous
                </Button>
                {currentQ < questions.length - 1 ? (
                  <Button variant="primary" onClick={() => setCurrentQ((q) => q + 1)}>
                    Next
                  </Button>
                ) : (
                  <Button variant="success" onClick={() => setSubmitOpen(true)}>
                    Review & Submit
                  </Button>
                )}
              </div>
            </motion.div>
          </div>
        )}

        {/* Camera feed */}
        <div className="w-64 border-l border-dark-800 bg-dark-900 flex-shrink-0 flex flex-col">
          <div className="p-3 border-b border-dark-800">
            <p className="text-xs font-semibold text-dark-400 uppercase tracking-wide">AI Proctor Monitor</p>
          </div>
          <div className="p-3 space-y-3 flex-1 overflow-y-auto">
            {attempt && !isSubmitted ? (
              <WebcamMonitor
                attemptId={attempt.id}
                examId={exam?.id}
                token={localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN) || ''}
                onStatusChange={(status) => {
                  setMonitorStatus(status);
                  if (status.cameraState === 'CAMERA_ACTIVE' && status.facesDetected >= 1) {
                    setIsFaceCaptured(true);
                  }
                }}
              />
            ) : isSubmitted ? (
              <div className="text-center p-4 text-xs text-dark-400 space-y-1">
                <CheckCircle className="h-5 w-5 text-green-400 mx-auto mb-1" />
                <p className="font-semibold text-white">Exam Submitted</p>
                <p className="text-[10px]">Monitoring successfully concluded</p>
              </div>
            ) : (
              <div className="text-center p-4 text-xs text-dark-400 space-y-2">
                <RefreshCw className="h-4 w-4 animate-spin mx-auto text-primary" />
                <p className="font-medium text-white">Connecting Exam Attempt...</p>
                <p className="text-[10px] text-dark-500">Preparing proctor session</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Submit confirm overlay */}
      {submitOpen && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-dark-900 border border-dark-700 rounded-2xl p-6 max-w-sm w-full mx-4"
          >
            <h3 className="font-display font-bold text-white text-lg mb-2">Submit Exam?</h3>
            <p className="text-dark-400 text-sm mb-4">
              You have answered {answered} of {questions.length} questions. Are you sure you want to submit?
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setSubmitOpen(false)}
                className="flex-1 py-2.5 rounded-lg border border-dark-600 text-dark-300 text-sm font-medium hover:bg-dark-800 transition-colors"
              >
                Continue Exam
              </button>
              <Button
                variant="primary"
                className="flex-1"
                onClick={handleSubmit}
              >
                Submit
              </Button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
