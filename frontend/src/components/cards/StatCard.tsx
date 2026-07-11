import React from 'react';
import { motion } from 'framer-motion';
import { TrendingUp, TrendingDown } from 'lucide-react';
import { cn, formatNumber, formatPercentage } from '@/utils';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  change?: number;
  changeLabel?: string;
  icon: LucideIcon;
  iconColor?: string;
  iconBg?: string;
  format?: 'number' | 'percentage' | 'currency' | 'raw';
  className?: string;
  delay?: number;
  onClick?: () => void;
}

export function StatCard({
  title,
  value,
  change,
  changeLabel,
  icon: Icon,
  iconColor = 'text-primary',
  iconBg = 'bg-primary/10',
  format = 'raw',
  className,
  delay = 0,
  onClick,
}: StatCardProps) {
  const formattedValue = () => {
    if (typeof value === 'string') return value;
    if (format === 'number') return formatNumber(value);
    if (format === 'percentage') return formatPercentage(value);
    if (format === 'currency') return `₹${formatNumber(value)}`;
    return String(value);
  };

  const isPositive = change !== undefined && change >= 0;
  const changeAbs = change !== undefined ? Math.abs(change) : undefined;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay }}
      onClick={onClick}
      className={cn(
        'rounded-xl border border-border bg-card p-5 shadow-soft',
        'transition-all duration-200',
        onClick && 'cursor-pointer hover:shadow-card hover:-translate-y-0.5',
        className
      )}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm text-muted-foreground font-medium">{title}</p>
          <p className="mt-2 text-3xl font-display font-bold tracking-tight text-foreground">
            {formattedValue()}
          </p>
          {change !== undefined && (
            <div className="mt-2 flex items-center gap-1.5">
              <span
                className={cn(
                  'inline-flex items-center gap-0.5 text-xs font-semibold',
                  isPositive ? 'text-success-600 dark:text-green-400' : 'text-danger-600 dark:text-red-400'
                )}
              >
                {isPositive ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
                {changeAbs !== undefined ? `${changeAbs.toFixed(1)}%` : ''}
              </span>
              {changeLabel && (
                <span className="text-xs text-muted-foreground">{changeLabel}</span>
              )}
            </div>
          )}
        </div>
        <div className={cn('flex h-11 w-11 items-center justify-center rounded-xl flex-shrink-0', iconBg)}>
          <Icon className={cn('h-5 w-5', iconColor)} />
        </div>
      </div>
    </motion.div>
  );
}
