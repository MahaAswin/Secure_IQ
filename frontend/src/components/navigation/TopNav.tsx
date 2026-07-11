import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Bell, Sun, Moon, Search, ChevronRight } from 'lucide-react';
import { cn } from '@/utils';
import { useAuth } from '@/contexts/AuthContext';
import { useTheme } from '@/contexts/ThemeContext';
import { Avatar } from '@/components/common/Avatar';
import { Badge } from '@/components/common/Badge';
import { ROLE_LABELS } from '@/constants';

function useBreadcrumbs() {
  const { pathname } = useLocation();
  const parts = pathname.split('/').filter(Boolean);
  return parts.map((part, i) => ({
    label: part.replace(/-/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()),
    href: '/' + parts.slice(0, i + 1).join('/'),
    isLast: i === parts.length - 1,
  }));
}

interface TopNavProps {
  className?: string;
}

export function TopNav({ className }: TopNavProps) {
  const { user } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const breadcrumbs = useBreadcrumbs();

  if (!user) return null;

  return (
    <header
      className={cn(
        'h-16 flex items-center justify-between px-6 border-b border-border bg-card/80 backdrop-blur-md flex-shrink-0',
        className
      )}
    >
      {/* Breadcrumbs */}
      <nav aria-label="breadcrumb" className="flex items-center gap-1">
        {breadcrumbs.map((crumb, i) => (
          <React.Fragment key={crumb.href}>
            {i > 0 && <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" />}
            {crumb.isLast ? (
              <span className="text-sm font-semibold text-foreground capitalize">{crumb.label}</span>
            ) : (
              <Link
                to={crumb.href}
                className="text-sm text-muted-foreground hover:text-foreground transition-colors capitalize"
              >
                {crumb.label}
              </Link>
            )}
          </React.Fragment>
        ))}
      </nav>

      {/* Right actions */}
      <div className="flex items-center gap-2">
        {/* Theme toggle */}
        <button
          onClick={toggleTheme}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
          aria-label="Toggle theme"
        >
          {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
        </button>

        {/* Notifications */}
        <button className="relative flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition-colors">
          <Bell className="h-4 w-4" />
          <span className="absolute top-1 right-1 h-2 w-2 rounded-full bg-danger animate-pulse" />
        </button>

        {/* User info */}
        <div className="flex items-center gap-2 pl-2 ml-1 border-l border-border">
          <Avatar name={user.name} size="sm" />
          <div className="hidden md:block">
            <p className="text-xs font-semibold text-foreground leading-none">{user.name}</p>
            <p className="text-[10px] text-muted-foreground mt-0.5">{ROLE_LABELS[user.role]}</p>
          </div>
        </div>
      </div>
    </header>
  );
}
