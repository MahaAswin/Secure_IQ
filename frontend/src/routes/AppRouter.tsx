import React, { Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';

import { ProtectedRoute, PublicRoute } from './ProtectedRoute';
import { UserRole } from '@/types';
import { ROUTES } from '@/constants';

// Layouts
import { AuthLayout } from '@/components/layout/AuthLayout';
import { DashboardLayout } from '@/components/layout/DashboardLayout';

// Pages
import { LandingPage } from '@/pages/landing/LandingPage';
import { LoginPage } from '@/pages/auth/LoginPage';
import { StudentDashboard } from '@/pages/student/StudentDashboard';
import { StudentExamsPage } from '@/pages/student/StudentExamsPage';
import { StudentResultsPage } from '@/pages/student/StudentResultsPage';
import { JoinExamPage } from '@/pages/student/JoinExamPage';
import { FacultyDashboard } from '@/pages/faculty/FacultyDashboard';
import { FacultyMonitoringPage } from '@/pages/faculty/FacultyMonitoringPage';
import { CreateExamPage } from '@/pages/faculty/CreateExamPage';
import { HODDashboard } from '@/pages/hod/HODDashboard';
import { AdminDashboard } from '@/pages/admin/AdminDashboard';
import { AnalyticsPage } from '@/pages/analytics/AnalyticsPage';
import { ReportsPage } from '@/pages/reports/ReportsPage';
import { AIReportPage } from '@/pages/reports/AIReportPage';
import { SecureBrowserPage } from '@/pages/secure-browser/SecureBrowserPage';
import { NotFoundPage } from '@/pages/NotFoundPage';

// Simple spinner fallback
function PageLoader() {
  return (
    <div className="flex h-screen items-center justify-center bg-background">
      <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
    </div>
  );
}

export function AppRouter() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        {/* Public landing */}
        <Route path={ROUTES.ROOT} element={<LandingPage />} />

        {/* Auth routes */}
        <Route element={<PublicRoute><AuthLayout /></PublicRoute>}>
          <Route path={ROUTES.LOGIN} element={<LoginPage />} />
        </Route>

        {/* Secure browser — full screen, no layout */}
        <Route
          path={ROUTES.SECURE_BROWSER}
          element={
            <ProtectedRoute allowedRoles={[UserRole.STUDENT]}>
              <SecureBrowserPage />
            </ProtectedRoute>
          }
        />

        {/* Student routes */}
        <Route
          element={
            <ProtectedRoute allowedRoles={[UserRole.STUDENT]}>
              <DashboardLayout />
            </ProtectedRoute>
          }
        >
          <Route path={ROUTES.STUDENT.DASHBOARD} element={<StudentDashboard />} />
          <Route path={ROUTES.STUDENT.EXAMS} element={<StudentExamsPage />} />
          <Route path={ROUTES.STUDENT.RESULTS} element={<StudentResultsPage />} />
          <Route path={ROUTES.STUDENT.JOIN_EXAM} element={<JoinExamPage />} />
        </Route>

        {/* Faculty routes */}
        <Route
          element={
            <ProtectedRoute allowedRoles={[UserRole.FACULTY]}>
              <DashboardLayout />
            </ProtectedRoute>
          }
        >
          <Route path={ROUTES.FACULTY.DASHBOARD} element={<FacultyDashboard />} />
          <Route path={ROUTES.FACULTY.CREATE_EXAM} element={<CreateExamPage />} />
          <Route path={ROUTES.FACULTY.MONITORING} element={<FacultyMonitoringPage />} />
          <Route path={ROUTES.FACULTY.ANALYTICS} element={<AnalyticsPage />} />
          <Route path={ROUTES.FACULTY.REPORTS} element={<ReportsPage />} />
          <Route path={ROUTES.FACULTY.EXAMS} element={<StudentExamsPage />} />
          <Route path={ROUTES.FACULTY.REPORT_DETAIL} element={<AIReportPage />} />
        </Route>

        {/* HOD routes */}
        <Route
          element={
            <ProtectedRoute allowedRoles={[UserRole.HOD]}>
              <DashboardLayout />
            </ProtectedRoute>
          }
        >
          <Route path={ROUTES.HOD.DASHBOARD} element={<HODDashboard />} />
          <Route path={ROUTES.HOD.FACULTY_PERFORMANCE} element={<HODDashboard />} />
          <Route path={ROUTES.HOD.ANALYTICS} element={<AnalyticsPage />} />
          <Route path={ROUTES.HOD.REPORTS} element={<ReportsPage />} />
        </Route>

        {/* Admin routes */}
        <Route
          element={
            <ProtectedRoute allowedRoles={[UserRole.ADMIN]}>
              <DashboardLayout />
            </ProtectedRoute>
          }
        >
          <Route path={ROUTES.ADMIN.DASHBOARD} element={<AdminDashboard />} />
          <Route path={ROUTES.ADMIN.DEPARTMENTS} element={<AdminDashboard />} />
          <Route path={ROUTES.ADMIN.FACULTY} element={<AdminDashboard />} />
          <Route path={ROUTES.ADMIN.STUDENTS} element={<AdminDashboard />} />
          <Route path={ROUTES.ADMIN.PERMISSIONS} element={<AdminDashboard />} />
          <Route path={ROUTES.ADMIN.SETTINGS} element={<AdminDashboard />} />
        </Route>

        {/* Shared protected routes */}
        <Route
          element={
            <ProtectedRoute>
              <DashboardLayout />
            </ProtectedRoute>
          }
        >
          <Route path={ROUTES.ANALYTICS} element={<AnalyticsPage />} />
          <Route path={ROUTES.REPORTS} element={<ReportsPage />} />
          <Route path={ROUTES.AI_REPORT} element={<AIReportPage />} />
        </Route>

        {/* 404 */}
        <Route path={ROUTES.NOT_FOUND} element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
