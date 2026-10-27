import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { AlertTriangle, User, Clock, Flag, RefreshCw, Eye, ShieldAlert } from 'lucide-react';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Timeline } from '@/components/common/Timeline';
import { formatRelativeTime, formatDate } from '@/utils';
import { cn } from '@/utils';
import api from '@/services/api';
import toast from 'react-hot-toast';
import { AlertSeverity } from '@/types';

export function FacultyMonitoringPage() {
  const [sessions, setSessions] = useState<any[]>([]);
  const [selectedSession, setSelectedSession] = useState<any>(null);
  const [attempts, setAttempts] = useState<any[]>([]);
  const [selectedAttempt, setSelectedAttempt] = useState<any>(null);
  const [violations, setViolations] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // 1. Initial sessions load
  async function loadSessions(isRefresh = false) {
    if (isRefresh) setRefreshing(true);
    try {
      const sessionsRes = await api.get('/v1/exam-sessions', { params: { size: 50 } });
      const sessionList = sessionsRes.data.data?.content || [];
      setSessions(sessionList);
      if (sessionList.length > 0) {
        if (!selectedSession || isRefresh) {
          const liveOrFirst = sessionList.find((s: any) => s.status === 'LIVE') || sessionList[0];
          setSelectedSession(liveOrFirst);
        }
      }
    } catch (err) {
      toast.error('Failed to load live monitoring sessions');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }

  useEffect(() => {
    loadSessions();
  }, []);

  // 2. Load attempts when selected session changes
  useEffect(() => {
    if (!selectedSession) return;
    async function loadAttempts() {
      try {
        const attemptsRes = await api.get('/v1/attempts', {
          params: { examSessionId: selectedSession.id, size: 100 }
        });
        const attemptList = attemptsRes.data.data?.content || [];
        setAttempts(attemptList);
        if (attemptList.length > 0) {
          setSelectedAttempt(attemptList[0]);
        } else {
          setSelectedAttempt(null);
          setViolations([]);
        }
      } catch (err) {
        toast.error('Failed to load attempts for active session');
      }
    }
    loadAttempts();

    // Auto-polling for active monitoring sessions every 8 seconds
    let interval: any = null;
    if (selectedSession?.status === 'LIVE') {
      interval = setInterval(() => {
        api.get('/v1/attempts', { params: { examSessionId: selectedSession.id, size: 100 } })
          .then((res) => {
            const list = res.data.data?.content || [];
            setAttempts(list);
            if (selectedAttempt) {
              const updatedCurrent = list.find((a: any) => a.id === selectedAttempt.id);
              if (updatedCurrent) setSelectedAttempt(updatedCurrent);
            }
          })
          .catch(() => {});
      }, 8000);
    }

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [selectedSession, selectedAttempt?.id]);

  // 3. Load violations when selected attempt changes
  useEffect(() => {
    if (!selectedAttempt) {
      setViolations([]);
      return;
    }
    async function loadViolations() {
      try {
        const violationsRes = await api.get('/v1/violations', {
          params: { studentExamAttemptId: selectedAttempt.id, size: 100 }
        });
        setViolations(violationsRes.data.data?.content || []);
      } catch {
        setViolations([]);
      }
    }
    loadViolations();

    // Auto-polling violations for selected attempt
    let pollInterval: any = null;
    if (selectedSession?.status === 'LIVE') {
      pollInterval = setInterval(loadViolations, 8000);
    }
    return () => {
      if (pollInterval) clearInterval(pollInterval);
    };
  }, [selectedAttempt, selectedSession?.status]);

  const handleRefresh = () => {
    loadSessions(true);
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  const timelineEvents = violations.map((v) => ({
    id: v.id.toString(),
    title: v.violationCode || 'AI Alert',
    description: `${v.description || 'Behavioral mismatch flagged.'} (Severity: ${v.severity})`,
    timestamp: formatRelativeTime(v.createdAt),
    severity: v.severity?.toUpperCase() === 'CRITICAL' ? AlertSeverity.CRITICAL :
              v.severity?.toUpperCase() === 'HIGH' ? AlertSeverity.HIGH :
              v.severity?.toUpperCase() === 'MEDIUM' ? AlertSeverity.MEDIUM :
              AlertSeverity.LOW,
  }));

  const getRiskScore = (att: any) => {
    const totalV = att.totalViolations || 0;
    return Math.min(100, totalV * 25);
  };

  const getRiskColor = (score: number) => {
    if (score < 30) return 'text-success';
    if (score < 60) return 'text-warning';
    return 'text-danger';
  };

  const getRiskLabel = (score: number) => {
    if (score < 30) return 'Low';
    if (score < 60) return 'Medium';
    return 'High';
  };

  const getStudentDisplayName = (student: any) => {
    if (!student) return 'Student';
    if (student.user) {
      const name = `${student.user.firstName || ''} ${student.user.lastName || ''}`.trim();
      if (name) return name;
    }
    return student.rollNumber || student.registerNumber || 'Student';
  };

  const selectedStudentName = selectedAttempt ? getStudentDisplayName(selectedAttempt.student) : '';

  return (
    <div className="space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">Live Exam Proctoring & Monitoring</h1>
          <p className="text-muted-foreground text-sm mt-1">
            {selectedSession ? `${selectedSession.sessionName} · ${attempts.length} active students connected` : 'No active sessions'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          {selectedSession?.status === 'LIVE' && <Badge variant="live" dot pulse>Live Session</Badge>}
          <Button
            variant="outline"
            size="sm"
            leftIcon={<RefreshCw className={`h-3.5 w-3.5 ${refreshing ? 'animate-spin' : ''}`} />}
            onClick={handleRefresh}
          >
            Refresh
          </Button>
        </div>
      </div>

      {/* Advisory Banner */}
      <div className="flex items-center gap-2.5 px-3 py-2 rounded-lg bg-primary/10 border border-primary/20 text-xs text-primary">
        <ShieldAlert className="h-4 w-4 flex-shrink-0" />
        <span>
          <strong>Faculty Advisory:</strong> AI camera detections are automated behavioral flags provided to assist proctors. They require human invigilator review and do not constitute conclusive proof of misconduct.
        </span>
      </div>

      {sessions.length === 0 ? (
        <Card className="p-8 text-center text-muted-foreground">
          <ShieldAlert className="h-10 w-10 mx-auto mb-3 text-muted-foreground/60" />
          <h3 className="font-display font-semibold text-foreground text-base">No Exam Sessions Found</h3>
          <p className="text-sm mt-1">Configure an exam and create a scheduled or live session to begin monitoring.</p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
          {/* Student grid */}
          <div className="lg:col-span-2 space-y-3">
            {/* Session selector */}
            <div className="flex gap-2 items-center mb-2">
              <label className="text-xs font-semibold text-muted-foreground uppercase">Select Session:</label>
              <select
                className="bg-card border border-border rounded px-2.5 py-1 text-sm text-foreground focus:outline-none"
                value={selectedSession?.id || ''}
                onChange={(e) => {
                  const s = sessions.find((x) => x.id.toString() === e.target.value);
                  setSelectedSession(s);
                }}
              >
                {sessions.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.sessionName} ({s.status})
                  </option>
                ))}
              </select>
            </div>

            <h2 className="font-semibold text-foreground text-sm">Active Students ({attempts.length})</h2>
            <div className="space-y-2">
              {attempts.length === 0 ? (
                <Card className="p-6 text-center text-muted-foreground">No students joined this session yet.</Card>
              ) : (
                attempts.map((att) => {
                  const score = getRiskScore(att);
                  const isSelected = selectedAttempt?.id === att.id;
                  const displayName = getStudentDisplayName(att.student);
                  return (
                    <motion.button
                      key={att.id}
                      onClick={() => setSelectedAttempt(att)}
                      whileHover={{ scale: 1.005 }}
                      className={cn(
                        'w-full flex items-center gap-4 p-4 rounded-xl border text-left transition-all duration-200',
                        isSelected
                          ? 'border-primary bg-primary/5 shadow-glow-sm'
                          : 'border-border bg-card hover:border-primary/30'
                      )}
                    >
                      {/* Avatar preview */}
                      <div className="h-16 w-20 rounded-lg bg-dark-900 flex items-center justify-center flex-shrink-0 relative overflow-hidden">
                        <User className="h-7 w-7 text-dark-500" />
                        {att.status === 'IN_PROGRESS' && (
                          <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-success animate-pulse" />
                        )}
                        {att.totalViolations > 0 && (
                          <span className="absolute top-1.5 left-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-danger text-white text-[9px] font-bold">
                            {att.totalViolations}
                          </span>
                        )}
                      </div>

                      {/* Info */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <p className="font-semibold text-foreground text-sm truncate">{displayName}</p>
                          {att.totalViolations > 0 && (
                            <Badge variant="danger" className="text-[10px]">
                              {att.totalViolations} flag{att.totalViolations > 1 ? 's' : ''}
                            </Badge>
                          )}
                          <Badge variant={att.status === 'IN_PROGRESS' ? 'live' : 'completed'} className="text-[10px]">
                            {att.status}
                          </Badge>
                        </div>
                        <p className="text-xs text-muted-foreground">
                          Roll No: {att.student?.rollNumber || '—'} · Code: <span className="font-mono">{att.attemptCode}</span>
                        </p>

                        {/* Risk bar */}
                        <div className="mt-2">
                          <div className="flex justify-between text-[10px] mb-1">
                            <span className="text-muted-foreground">Risk Level</span>
                            <span className={cn('font-bold', getRiskColor(score))}>
                              {score}% · {getRiskLabel(score)}
                            </span>
                          </div>
                          <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
                            <div
                              className={cn(
                                'h-full rounded-full transition-all duration-500',
                                score < 30 ? 'bg-success' : score < 60 ? 'bg-warning' : 'bg-danger'
                              )}
                              style={{ width: `${score}%` }}
                            />
                          </div>
                        </div>
                      </div>
                    </motion.button>
                  );
                })
              )}
            </div>
          </div>

          {/* Right column: Selected student violations */}
          <div className="space-y-4">
            <Card>
              <CardHeader
                title="Student Audit Feed"
                description={selectedStudentName ? `Live log for ${selectedStudentName}` : 'Select a student'}
                icon={<AlertTriangle className="h-4 w-4" />}
              />
              <div className="space-y-3">
                {violations.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-6">
                    {selectedAttempt ? 'No security violations detected. Clean proctored session.' : 'Select a student to view activity.'}
                  </p>
                ) : (
                  <Timeline events={timelineEvents} />
                )}
              </div>
            </Card>

            {selectedAttempt && (
              <Card className="p-4 space-y-2 text-xs">
                <h4 className="font-semibold text-foreground text-sm">Attempt Metadata</h4>
                <div className="flex justify-between py-1 border-b border-border">
                  <span className="text-muted-foreground">Student</span>
                  <span className="font-medium text-foreground">{selectedStudentName}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-border">
                  <span className="text-muted-foreground">Status</span>
                  <span className="font-medium text-foreground">{selectedAttempt.status}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-border">
                  <span className="text-muted-foreground">Started At</span>
                  <span className="font-medium text-foreground">{selectedAttempt.startTime ? formatDate(selectedAttempt.startTime) : '—'}</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-muted-foreground">Total Flags</span>
                  <span className="font-bold text-danger">{selectedAttempt.totalViolations || 0}</span>
                </div>
              </Card>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
