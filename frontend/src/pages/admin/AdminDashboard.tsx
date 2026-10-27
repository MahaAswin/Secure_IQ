import React, { useState, useEffect } from 'react';
import { Building2, Users, GraduationCap, BarChart3, Plus, Edit, Trash2, AlertTriangle, ShieldCheck } from 'lucide-react';
import { StatCard } from '@/components/cards/StatCard';
import { DataTable, Column } from '@/components/tables/DataTable';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { ConfirmDialog } from '@/components/dialogs/Dialog';
import api from '@/services/api';
import toast from 'react-hot-toast';
import { cn } from '@/utils';

const tabs = ['Departments', 'Students', 'Faculty'] as const;
type Tab = typeof tabs[number];

const deptColumns: Column<any>[] = [
  { key: 'name', header: 'Department', sortable: true },
  { key: 'code', header: 'Code', align: 'center', render: (v) => <Badge variant="primary">{String(v)}</Badge> },
  { key: 'hodName', header: 'HOD', render: (v) => v ? String(v) : '—' },
  { key: 'facultyCount', header: 'Faculty', align: 'center', sortable: true },
  { key: 'studentCount', header: 'Students', align: 'center', sortable: true },
  {
    key: 'activeExams',
    header: 'Active Exams',
    align: 'center',
    render: (v) => Number(v) > 0 ? <Badge variant="live" dot>{String(v)}</Badge> : <span className="text-muted-foreground">0</span>,
  },
];

const studentColumns: Column<any>[] = [
  { key: 'name', header: 'Name', sortable: true },
  { key: 'rollNumber', header: 'Roll No.', render: (v) => <span className="font-mono text-xs">{String(v)}</span> },
  { key: 'email', header: 'Email', render: (v) => <span className="text-xs text-muted-foreground">{String(v || '—')}</span> },
  { key: 'department', header: 'Department', sortable: true },
  { key: 'semester', header: 'Sem', align: 'center' },
  {
    key: 'isActive',
    header: 'Status',
    align: 'center',
    render: (v) => <Badge variant={v ? 'success' : 'cancelled'}>{v ? 'Active' : 'Inactive'}</Badge>,
  },
];

const facultyColumns: Column<any>[] = [
  { key: 'name', header: 'Name', sortable: true },
  { key: 'employeeId', header: 'Emp ID', render: (v) => <span className="font-mono text-xs">{String(v)}</span> },
  { key: 'email', header: 'Email', render: (v) => <span className="text-xs text-muted-foreground">{String(v || '—')}</span> },
  { key: 'designation', header: 'Designation', sortable: true },
  { key: 'department', header: 'Department', sortable: true },
  { key: 'isActive', header: 'Status', align: 'center', render: (v) => <Badge variant={v ? 'success' : 'cancelled'}>{v ? 'Active' : 'Inactive'}</Badge> },
];

export function AdminDashboard() {
  const [tab, setTab] = useState<Tab>('Departments');
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [departments, setDepartments] = useState<any[]>([]);
  const [students, setStudents] = useState<any[]>([]);
  const [faculty, setFaculty] = useState<any[]>([]);
  const [totalUsers, setTotalUsers] = useState(0);
  const [totalExams, setTotalExams] = useState(0);
  const [activeSessions, setActiveSessions] = useState(0);
  const [violationsCount, setViolationsCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadAdminData() {
      try {
        const [
          deptsRes,
          studentsRes,
          facultyRes,
          usersRes,
          examsRes,
          sessionsRes,
          violationsRes
        ] = await Promise.allSettled([
          api.get('/v1/departments', { params: { size: 100 } }),
          api.get('/v1/students', { params: { size: 100 } }),
          api.get('/v1/faculties', { params: { size: 100 } }),
          api.get('/v1/users', { params: { size: 1 } }),
          api.get('/v1/exams', { params: { size: 1 } }),
          api.get('/v1/exam-sessions', { params: { status: 'LIVE', size: 1 } }),
          api.get('/v1/violations/dashboard'),
        ]);

        if (deptsRes.status === 'fulfilled') {
          const depts = deptsRes.value.data.data?.content || [];
          setDepartments(depts.map((d: any) => ({
            id: d.id,
            name: d.departmentName || d.name,
            code: d.departmentCode,
            hodName: d.hodName || '—',
            facultyCount: d.facultyCount || 0,
            studentCount: d.studentCount || 0,
            activeExams: d.activeExamsCount || 0
          })));
        }

        if (studentsRes.status === 'fulfilled') {
          const studs = studentsRes.value.data.data?.content || [];
          setStudents(studs.map((s: any) => {
            const fullName = s.user ? `${s.user.firstName || ''} ${s.user.lastName || ''}`.trim() : '';
            return {
              id: s.id,
              name: fullName || s.rollNumber || 'Student',
              rollNumber: s.rollNumber,
              email: s.user?.email || '—',
              department: s.department?.departmentName || s.department?.name || 'N/A',
              semester: s.semester || 1,
              isActive: s.user?.status === 'ACTIVE'
            };
          }));
        }

        if (facultyRes.status === 'fulfilled') {
          const facs = facultyRes.value.data.data?.content || [];
          setFaculty(facs.map((f: any) => {
            const fullName = f.user ? `${f.user.firstName || ''} ${f.user.lastName || ''}`.trim() : '';
            return {
              id: f.id,
              name: fullName || f.employeeId || 'Faculty',
              employeeId: f.employeeId,
              email: f.user?.email || '—',
              designation: f.designation || 'Faculty Member',
              department: f.department?.departmentName || f.department?.name || 'N/A',
              isActive: f.user?.status === 'ACTIVE'
            };
          }));
        }

        if (usersRes.status === 'fulfilled') {
          setTotalUsers(usersRes.value.data.data?.totalElements || 0);
        }

        if (examsRes.status === 'fulfilled') {
          setTotalExams(examsRes.value.data.data?.totalElements || 0);
        }

        if (sessionsRes.status === 'fulfilled') {
          setActiveSessions(sessionsRes.value.data.data?.totalElements || 0);
        }

        if (violationsRes.status === 'fulfilled') {
          setViolationsCount(violationsRes.value.data.data?.totalViolationsCount || 0);
        }

      } catch (err) {
        toast.error('Failed to load admin metrics');
      } finally {
        setLoading(false);
      }
    }
    loadAdminData();
  }, []);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  const calculatedStudents = students.length;
  const calculatedFaculty = faculty.length;

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">Admin Dashboard</h1>
          <p className="text-muted-foreground text-sm mt-1">Real-time system overview and directory management</p>
        </div>
      </div>

      {/* Primary Metrics Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard title="Total Users" value={totalUsers || (calculatedStudents + calculatedFaculty)} icon={Users} delay={0} />
        <StatCard title="Departments" value={departments.length} icon={Building2} iconBg="bg-secondary/10" iconColor="text-secondary" delay={0.05} />
        <StatCard title="Total Students" value={calculatedStudents} icon={Users} format="number" iconBg="bg-primary/10" iconColor="text-primary" delay={0.1} />
        <StatCard title="Total Faculty" value={calculatedFaculty} icon={GraduationCap} iconBg="bg-success/10" iconColor="text-success" delay={0.15} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <StatCard title="Total Exams Configured" value={totalExams} icon={BarChart3} delay={0.2} />
        <StatCard title="Active Live Sessions" value={activeSessions} icon={ShieldCheck} iconBg="bg-primary/10" iconColor="text-primary" delay={0.25} />
        <StatCard title="Total Violations Flagged" value={violationsCount} icon={AlertTriangle} iconBg="bg-danger/10" iconColor="text-danger" delay={0.3} />
      </div>

      {/* Tab navigation */}
      <div className="flex gap-1 border-b border-border">
        {tabs.map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={cn(
              'px-4 py-2.5 text-sm font-medium transition-colors border-b-2 -mb-px',
              tab === t
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            )}
          >
            {t}
          </button>
        ))}
      </div>

      {/* Tab content */}
      {tab === 'Departments' && (
        <DataTable
          data={departments}
          columns={deptColumns}
          keyField="id"
          searchable
          searchFields={['name', 'code', 'hodName']}
        />
      )}

      {tab === 'Students' && (
        <DataTable
          data={students}
          columns={studentColumns}
          keyField="id"
          searchable
          searchFields={['name', 'rollNumber', 'email', 'department']}
        />
      )}

      {tab === 'Faculty' && (
        <DataTable
          data={faculty}
          columns={facultyColumns}
          keyField="id"
          searchable
          searchFields={['name', 'employeeId', 'email', 'designation', 'department']}
        />
      )}
    </div>
  );
}
