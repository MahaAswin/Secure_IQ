import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/utils';

const badgeVariants = cva(
  'inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold transition-colors',
  {
    variants: {
      variant: {
        default: 'bg-dark-100 text-dark-700 dark:bg-dark-800 dark:text-dark-300',
        primary: 'bg-primary/10 text-primary-700 dark:bg-primary/20 dark:text-primary-400',
        secondary: 'bg-secondary/10 text-secondary-700 dark:bg-secondary/20 dark:text-secondary-400',
        success: 'bg-success/10 text-success-600 dark:bg-success/20 dark:text-green-400',
        warning: 'bg-warning/10 text-warning-600 dark:bg-warning/20 dark:text-amber-400',
        danger: 'bg-danger/10 text-danger-600 dark:bg-danger/20 dark:text-red-400',
        outline: 'border border-border text-muted-foreground',
        live: 'bg-danger/10 text-danger-600 dark:text-red-400',
        upcoming: 'bg-primary/10 text-primary dark:text-primary-400',
        completed: 'bg-success/10 text-success-600 dark:text-green-400',
        cancelled: 'bg-dark-100 text-dark-500 dark:bg-dark-800',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {
  dot?: boolean;
  pulse?: boolean;
}

export function Badge({ className, variant, dot, pulse, children, ...props }: BadgeProps) {
  return (
    <span className={cn(badgeVariants({ variant, className }))} {...props}>
      {dot && (
        <span
          className={cn(
            'h-1.5 w-1.5 rounded-full',
            pulse && 'animate-pulse',
            variant === 'live' && 'bg-danger-600',
            variant === 'success' && 'bg-success-600',
            variant === 'warning' && 'bg-warning-500',
            variant === 'primary' && 'bg-primary',
            (!variant || variant === 'default') && 'bg-dark-400'
          )}
        />
      )}
      {children}
    </span>
  );
}
