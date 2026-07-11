import React from 'react';
import { cn } from '@/utils';
import { motion } from 'framer-motion';

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  padding?: 'none' | 'sm' | 'md' | 'lg';
  hover?: boolean;
  glass?: boolean;
  gradient?: boolean;
}

const paddingMap = {
  none: '',
  sm: 'p-4',
  md: 'p-5',
  lg: 'p-6',
};

export function Card({
  className,
  padding = 'md',
  hover = false,
  glass = false,
  gradient = false,
  children,
  ...props
}: CardProps) {
  return (
    <div
      className={cn(
        'rounded-xl border border-border bg-card shadow-soft',
        paddingMap[padding],
        hover && 'transition-all duration-200 hover:shadow-card hover:-translate-y-0.5 cursor-pointer',
        glass && 'glass',
        gradient && 'bg-gradient-subtle',
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}

interface CardHeaderProps extends React.HTMLAttributes<HTMLDivElement> {
  title: string;
  description?: string;
  action?: React.ReactNode;
  icon?: React.ReactNode;
}

export function CardHeader({ title, description, action, icon, className, ...props }: CardHeaderProps) {
  return (
    <div className={cn('flex items-start justify-between mb-5', className)} {...props}>
      <div className="flex items-center gap-3">
        {icon && (
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary flex-shrink-0">
            {icon}
          </div>
        )}
        <div>
          <h3 className="font-display font-semibold text-foreground text-base">{title}</h3>
          {description && <p className="text-xs text-muted-foreground mt-0.5">{description}</p>}
        </div>
      </div>
      {action && <div className="flex-shrink-0">{action}</div>}
    </div>
  );
}

interface AnimatedCardProps extends CardProps {
  delay?: number;
}

export function AnimatedCard({ delay = 0, children, ...props }: AnimatedCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay }}
    >
      <Card {...props}>{children}</Card>
    </motion.div>
  );
}
