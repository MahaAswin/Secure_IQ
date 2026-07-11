import React from 'react';
import { cn, getInitials, getAvatarColor } from '@/utils';

interface AvatarProps {
  name: string;
  src?: string;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
  showBorder?: boolean;
}

const sizeMap = {
  xs: 'h-6 w-6 text-xs',
  sm: 'h-8 w-8 text-sm',
  md: 'h-10 w-10 text-sm',
  lg: 'h-12 w-12 text-base',
  xl: 'h-16 w-16 text-lg',
};

export function Avatar({ name, src, size = 'md', className, showBorder }: AvatarProps) {
  const colorClass = getAvatarColor(name);
  const initials = getInitials(name);

  return (
    <span
      className={cn(
        'relative inline-flex items-center justify-center rounded-full overflow-hidden flex-shrink-0',
        sizeMap[size],
        showBorder && 'ring-2 ring-white dark:ring-dark-800',
        !src && `bg-gradient-to-br ${colorClass}`,
        className
      )}
    >
      {src ? (
        <img src={src} alt={name} className="h-full w-full object-cover" />
      ) : (
        <span className="font-semibold text-white leading-none">{initials}</span>
      )}
    </span>
  );
}

interface AvatarGroupProps {
  items: { name: string; src?: string }[];
  max?: number;
  size?: AvatarProps['size'];
  className?: string;
}

export function AvatarGroup({ items, max = 4, size = 'sm', className }: AvatarGroupProps) {
  const visible = items.slice(0, max);
  const overflow = items.length - max;

  return (
    <div className={cn('flex items-center', className)}>
      {visible.map((item, i) => (
        <Avatar
          key={i}
          name={item.name}
          src={item.src}
          size={size}
          showBorder
          className="-ml-2 first:ml-0"
        />
      ))}
      {overflow > 0 && (
        <span
          className={cn(
            '-ml-2 inline-flex items-center justify-center rounded-full bg-muted text-muted-foreground font-semibold ring-2 ring-white dark:ring-dark-800',
            sizeMap[size],
            'text-xs'
          )}
        >
          +{overflow}
        </span>
      )}
    </div>
  );
}
