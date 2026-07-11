import React from 'react';
import { cn } from '@/utils';
import { AlertSeverity } from '@/types';

interface TimelineEvent {
  id: string;
  title: string;
  description?: string;
  timestamp: string;
  severity?: AlertSeverity;
  icon?: React.ReactNode;
}

interface TimelineProps {
  events: TimelineEvent[];
  className?: string;
}

const severityColorMap: Record<AlertSeverity, string> = {
  [AlertSeverity.LOW]: 'bg-success text-white',
  [AlertSeverity.MEDIUM]: 'bg-warning text-white',
  [AlertSeverity.HIGH]: 'bg-danger text-white',
  [AlertSeverity.CRITICAL]: 'bg-danger-700 text-white animate-pulse',
};

const severityLineMap: Record<AlertSeverity, string> = {
  [AlertSeverity.LOW]: 'border-success/30',
  [AlertSeverity.MEDIUM]: 'border-warning/30',
  [AlertSeverity.HIGH]: 'border-danger/30',
  [AlertSeverity.CRITICAL]: 'border-danger/50',
};

export function Timeline({ events, className }: TimelineProps) {
  return (
    <div className={cn('relative space-y-0', className)}>
      {events.map((event, index) => (
        <div key={event.id} className="flex gap-4 group">
          {/* Line + dot */}
          <div className="flex flex-col items-center">
            <div
              className={cn(
                'h-8 w-8 rounded-full flex items-center justify-center flex-shrink-0 text-xs font-bold z-10',
                event.severity
                  ? severityColorMap[event.severity]
                  : 'bg-primary/10 text-primary'
              )}
            >
              {event.icon ?? String(index + 1)}
            </div>
            {index < events.length - 1 && (
              <div
                className={cn(
                  'flex-1 w-px border-l-2 border-dashed my-1',
                  event.severity ? severityLineMap[event.severity] : 'border-border'
                )}
              />
            )}
          </div>

          {/* Content */}
          <div className={cn('pb-5 flex-1', index === events.length - 1 && 'pb-0')}>
            <div className="flex items-start justify-between gap-2">
              <p className="text-sm font-semibold text-foreground">{event.title}</p>
              <span className="text-xs text-muted-foreground whitespace-nowrap">{event.timestamp}</span>
            </div>
            {event.description && (
              <p className="mt-0.5 text-xs text-muted-foreground">{event.description}</p>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
