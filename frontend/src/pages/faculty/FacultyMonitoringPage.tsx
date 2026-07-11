import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { AlertTriangle, User, Clock, Flag, RefreshCw, Eye } from 'lucide-react';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Timeline } from '@/components/common/Timeline';
import { mockMonitoringSessions, mockExams } from '@/services/mockData';
import { AlertSeverity, MonitoringSession } from '@/types';
import { formatRelativeTime, getRiskColor, getRiskLabel } from '@/utils';
import { cn } from '@/utils';

const severityBadge: Record<AlertSeverity, 'danger' | 'warning' | 'success' | 'primary'> = {
  [AlertSeverity.CRITICAL]: 'danger',
  [AlertSeverity.HIGH]: 'danger',
  [AlertSeverity.MEDIUM]: 'warning',
  [AlertSeverity.LOW]: 'success',
};

export function FacultyMonitoringPage() {
  const [selected, setSelected] = useState<MonitoringSession>(mockMonitoringSessions[0]);
  const liveExam = mockExams[0];

  const timelineEvents = selected.flags.map((flag) => ({
    id: flag.id,
    title: flag.type,
    description: `${flag.description} (Confidence: ${(flag.confidence * 100).toFixed(0)}%)`,
    timestamp: formatRelativeTime(flag.timestamp),
    severity: flag.severity,
  }));

  return (
    <div className="space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">Live Monitoring</h1>
          <p className="text-muted-foreground text-sm mt-1">{liveExam.title} · {mockMonitoringSessions.length} students active</p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="live" dot pulse>Live</Badge>
          <Button variant="outline" size="sm" leftIcon={<RefreshCw className="h-3.5 w-3.5" />}>
            Refresh
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* Student grid */}
        <div className="lg:col-span-2 space-y-3">
          <h2 className="font-semibold text-foreground text-sm">Active Students ({mockMonitoringSessions.length})</h2>
          <div className="space-y-2">
            {mockMonitoringSessions.map((session) => (
              <motion.button
                key={session.id}
                onClick={() => setSelected(session)}
                whileHover={{ scale: 1.005 }}
                className={cn(
                  'w-full flex items-center gap-4 p-4 rounded-xl border text-left transition-all duration-200',
                  selected.id === session.id
                    ? 'border-primary bg-primary/5 shadow-glow-sm'
                    : 'border-border bg-card hover:border-primary/30'
                )}
              >
                {/* Camera placeholder */}
                <div className="h-20 w-28 rounded-lg bg-dark-900 flex items-center justify-center flex-shrink-0 relative overflow-hidden">
                  <User className="h-8 w-8 text-dark-600" />
                  {session.isActive && (
                    <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-success animate-pulse" />
                  )}
                  {session.flagCount > 0 && (
                    <span className="absolute top-1.5 left-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-danger text-white text-[10px] font-bold">
                      {session.flagCount}
                    </span>
                  )}
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <p className="font-semibold text-foreground text-sm">{session.studentName}</p>
                    {session.flagCount > 0 && (
                      <Badge variant="danger" className="text-[10px]">
                        {session.flagCount} flag{session.flagCount > 1 ? 's' : ''}
                      </Badge>
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground">Last activity: {formatRelativeTime(session.lastActivity)}</p>

                  {/* Risk bar */}
                  <div className="mt-2">
                    <div className="flex justify-between text-[10px] mb-1">
                      <span className="text-muted-foreground">Risk Score</span>
                      <span className={cn('font-bold', getRiskColor(session.riskScore))}>
                        {session.riskScore}% · {getRiskLabel(session.riskScore)}
                      </span>
                    </div>
                    <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
                      <div
                        className={cn(
                          'h-full rounded-full transition-all duration-500',
                          session.riskScore < 30 ? 'bg-success' : session.riskScore < 60 ? 'bg-warning' : 'bg-danger'
                        )}
                        style={{ width: `${session.riskScore}%` }}
                      />
                    </div>
                  </div>
                </div>

                <Eye className={cn('h-4 w-4 flex-shrink-0', selected.id === session.id ? 'text-primary' : 'text-muted-foreground')} />
              </motion.button>
            ))}
          </div>
        </div>

        {/* Detail panel */}
        <div className="space-y-4">
          <Card>
            <CardHeader
              title={selected.studentName}
              description={`Risk: ${selected.riskScore}%`}
              icon={<Flag className="h-4 w-4" />}
            />
            {/* Camera feed mockup */}
            <div className="aspect-video rounded-lg bg-dark-900 flex items-center justify-center mb-4">
              <div className="text-center">
                <User className="h-12 w-12 text-dark-600 mx-auto" />
                <p className="text-dark-500 text-xs mt-2">Camera Feed</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2 mb-4 text-center">
              <div className="p-2 rounded-lg bg-muted/40">
                <p className="font-bold text-foreground text-lg">{selected.flagCount}</p>
                <p className="text-xs text-muted-foreground">Flags</p>
              </div>
              <div className="p-2 rounded-lg bg-muted/40">
                <p className={cn('font-bold text-lg', getRiskColor(selected.riskScore))}>{selected.riskScore}%</p>
                <p className="text-xs text-muted-foreground">Risk Score</p>
              </div>
            </div>

            {timelineEvents.length > 0 ? (
              <Timeline events={timelineEvents} />
            ) : (
              <p className="text-sm text-muted-foreground text-center py-4">No flags detected</p>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
