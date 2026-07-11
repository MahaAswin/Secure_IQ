import React from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/utils';
import { LucideIcon } from 'lucide-react';
import { Button } from './Button';

interface EmptyStateProps {
  icon?: LucideIcon;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
  size?: 'sm' | 'md' | 'lg';
}

export function EmptyState({ icon: Icon, title, description, action, className, size = 'md' }: EmptyStateProps) {
  const sizeMap = {
    sm: { wrapper: 'py-8 px-4', icon: 'h-8 w-8', iconWrap: 'h-14 w-14', title: 'text-base', desc: 'text-sm' },
    md: { wrapper: 'py-12 px-6', icon: 'h-10 w-10', iconWrap: 'h-18 w-18', title: 'text-lg', desc: 'text-sm' },
    lg: { wrapper: 'py-16 px-8', icon: 'h-12 w-12', iconWrap: 'h-20 w-20', title: 'text-xl', desc: 'text-base' },
  };
  const s = sizeMap[size];

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className={cn(
        'flex flex-col items-center justify-center text-center',
        s.wrapper,
        className
      )}
    >
      {Icon && (
        <div
          className={cn(
            'flex items-center justify-center rounded-2xl bg-muted mb-4',
            size === 'sm' ? 'h-14 w-14' : size === 'lg' ? 'h-20 w-20' : 'h-16 w-16'
          )}
        >
          <Icon className={cn(s.icon, 'text-muted-foreground')} />
        </div>
      )}
      <h3 className={cn('font-display font-semibold text-foreground', s.title)}>{title}</h3>
      {description && (
        <p className={cn('mt-1.5 text-muted-foreground max-w-sm', s.desc)}>{description}</p>
      )}
      {action && (
        <div className="mt-5">
          <Button variant="primary" onClick={action.onClick} size="sm">
            {action.label}
          </Button>
        </div>
      )}
    </motion.div>
  );
}
