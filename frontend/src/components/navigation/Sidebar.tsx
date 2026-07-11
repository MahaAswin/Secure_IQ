import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  LayoutDashboard, BookOpen, ClipboardList, BarChart3, Settings,
  Users, Building2, ChevronLeft, ChevronRight, LogOut, Shield,
  MonitorPlay, FileText, GraduationCap, Activity, Award, Bell,
  ChevronDown,
} from 'lucide-react';
import { cn } from '@/utils';
import { useAuth } from '@/contexts/AuthContext';
import { UserRole } from '@/types';
import { ROUTES } from '@/constants';
import { Avatar } from '@/components/common/Avatar';
import { Badge } from '@/components/common/Badge';

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: string | number;
  children?: NavItem[];
}

const studentNav: NavItem[] = [
  { label: 'Dashboard', href: ROUTES.STUDENT.DASHBOARD, icon: LayoutDashboard },
  { label: 'My Exams', href: ROUTES.STUDENT.EXAMS, icon: BookOpen },
  { label: 'Results', href: ROUTES.STUDENT.RESULTS, icon: Award },
];

const facultyNav: NavItem[] = [
  { label: 'Dashboard', href: ROUTES.FACULTY.DASHBOARD, icon: LayoutDashboard },
  { label: 'Exams', href: ROUTES.FACULTY.EXAMS, icon: BookOpen },
  { label: 'Live Monitoring', href: ROUTES.FACULTY.MONITORING, icon: MonitorPlay, badge: 'Live' },
  { label: 'Analytics', href: ROUTES.FACULTY.ANALYTICS, icon: BarChart3 },
  { label: 'Reports', href: ROUTES.FACULTY.REPORTS, icon: FileText },
];

const hodNav: NavItem[] = [
  { label: 'Dashboard', href: ROUTES.HOD.DASHBOARD, icon: LayoutDashboard },
  { label: 'Faculty Performance', href: ROUTES.HOD.FACULTY_PERFORMANCE, icon: Users },
  { label: 'Analytics', href: ROUTES.HOD.ANALYTICS, icon: BarChart3 },
  { label: 'Reports', href: ROUTES.HOD.REPORTS, icon: FileText },
];

const adminNav: NavItem[] = [
  { label: 'Dashboard', href: ROUTES.ADMIN.DASHBOARD, icon: LayoutDashboard },
  { label: 'Departments', href: ROUTES.ADMIN.DEPARTMENTS, icon: Building2 },
  { label: 'Faculty', href: ROUTES.ADMIN.FACULTY, icon: GraduationCap },
  { label: 'Students', href: ROUTES.ADMIN.STUDENTS, icon: Users },
  { label: 'Permissions', href: ROUTES.ADMIN.PERMISSIONS, icon: Shield },
  { label: 'Settings', href: ROUTES.ADMIN.SETTINGS, icon: Settings },
];

const navByRole: Record<UserRole, NavItem[]> = {
  [UserRole.STUDENT]: studentNav,
  [UserRole.FACULTY]: facultyNav,
  [UserRole.HOD]: hodNav,
  [UserRole.ADMIN]: adminNav,
};

const roleColor: Record<UserRole, string> = {
  [UserRole.STUDENT]: 'text-primary',
  [UserRole.FACULTY]: 'text-secondary-600',
  [UserRole.HOD]: 'text-success-600',
  [UserRole.ADMIN]: 'text-danger-600',
};

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export function Sidebar({ collapsed, onToggle }: SidebarProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [expandedItems, setExpandedItems] = useState<string[]>([]);

  if (!user) return null;

  const navItems = navByRole[user.role];

  const handleLogout = () => {
    logout();
    navigate(ROUTES.LOGIN);
  };

  const toggleExpand = (label: string) => {
    setExpandedItems((prev) =>
      prev.includes(label) ? prev.filter((l) => l !== label) : [...prev, label]
    );
  };

  return (
    <motion.aside
      animate={{ width: collapsed ? 64 : 260 }}
      transition={{ duration: 0.25, ease: 'easeInOut' }}
      className="relative flex flex-col h-full bg-card border-r border-border overflow-hidden flex-shrink-0"
    >
      {/* Logo */}
      <div className="flex items-center h-16 px-4 border-b border-border flex-shrink-0">
        <div className="flex items-center gap-3 overflow-hidden">
          <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-gradient-primary text-white font-bold text-sm font-display">
            S
          </div>
          <AnimatePresence>
            {!collapsed && (
              <motion.div
                initial={{ opacity: 0, x: -8 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -8 }}
                transition={{ duration: 0.2 }}
                className="overflow-hidden"
              >
                <p className="font-display font-bold text-foreground text-base leading-none">SecureIQ</p>
                <p className="text-[10px] text-muted-foreground mt-0.5">Secure Exams. AI Insights.</p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto scrollbar-none px-2 py-4 space-y-0.5">
        {navItems.map((item) => {
          const Icon = item.icon;
          const hasChildren = item.children && item.children.length > 0;
          const isExpanded = expandedItems.includes(item.label);

          return (
            <div key={item.label}>
              {hasChildren ? (
                <button
                  onClick={() => toggleExpand(item.label)}
                  className={cn(
                    'w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200',
                    'text-dark-500 hover:bg-primary/8 hover:text-primary dark:text-dark-300'
                  )}
                >
                  <Icon className={cn('h-4 w-4 flex-shrink-0', collapsed && 'mx-auto')} />
                  {!collapsed && (
                    <>
                      <span className="flex-1 text-left">{item.label}</span>
                      <ChevronDown className={cn('h-3.5 w-3.5 transition-transform', isExpanded && 'rotate-180')} />
                    </>
                  )}
                </button>
              ) : (
                <NavLink
                  to={item.href}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200',
                      isActive
                        ? 'bg-primary/10 text-primary dark:bg-primary/20 dark:text-primary-400 font-semibold'
                        : 'text-dark-500 hover:bg-primary/8 hover:text-primary dark:text-dark-300 dark:hover:text-primary-400'
                    )
                  }
                  title={collapsed ? item.label : undefined}
                >
                  <Icon className={cn('h-4 w-4 flex-shrink-0', collapsed && 'mx-auto')} />
                  {!collapsed && (
                    <>
                      <span className="flex-1">{item.label}</span>
                      {item.badge && (
                        <Badge variant="live" dot pulse className="text-[10px] px-1.5">
                          {item.badge}
                        </Badge>
                      )}
                    </>
                  )}
                </NavLink>
              )}

              {/* Expanded children */}
              <AnimatePresence>
                {hasChildren && isExpanded && !collapsed && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: 'auto', opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.2 }}
                    className="overflow-hidden ml-4 mt-0.5 border-l border-border pl-2 space-y-0.5"
                  >
                    {item.children?.map((child) => {
                      const ChildIcon = child.icon;
                      return (
                        <NavLink
                          key={child.label}
                          to={child.href}
                          className={({ isActive }) =>
                            cn(
                              'flex items-center gap-2.5 px-2.5 py-2 rounded-lg text-sm transition-all duration-200',
                              isActive
                                ? 'bg-primary/10 text-primary font-semibold'
                                : 'text-dark-500 hover:bg-primary/8 hover:text-primary dark:text-dark-300'
                            )
                          }
                        >
                          <ChildIcon className="h-3.5 w-3.5 flex-shrink-0" />
                          <span>{child.label}</span>
                        </NavLink>
                      );
                    })}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          );
        })}
      </nav>

      {/* User */}
      <div className="border-t border-border p-3 flex-shrink-0">
        <div className="flex items-center gap-3">
          <Avatar name={user.name} size="sm" className="flex-shrink-0" />
          <AnimatePresence>
            {!collapsed && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex-1 min-w-0"
              >
                <p className="text-sm font-semibold text-foreground truncate">{user.name}</p>
                <p className={cn('text-[10px] font-medium uppercase tracking-wide', roleColor[user.role])}>
                  {user.role}
                </p>
              </motion.div>
            )}
          </AnimatePresence>
          {!collapsed && (
            <button
              onClick={handleLogout}
              title="Logout"
              className="text-muted-foreground hover:text-danger transition-colors p-1 rounded-md hover:bg-danger/10"
            >
              <LogOut className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>

      {/* Toggle button */}
      <button
        onClick={onToggle}
        className="absolute -right-3 top-20 z-10 flex h-6 w-6 items-center justify-center rounded-full border border-border bg-card shadow-soft text-muted-foreground hover:text-foreground hover:bg-muted transition-all"
      >
        {collapsed ? <ChevronRight className="h-3 w-3" /> : <ChevronLeft className="h-3 w-3" />}
      </button>
    </motion.aside>
  );
}
