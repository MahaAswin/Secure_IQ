import React, { useState } from 'react';
import { Building2, Users, GraduationCap, Settings, BarChart3, Plus, Edit, Trash2, Shield } from 'lucide-react';
import { StatCard } from '@/components/cards/StatCard';
import { Card, CardHeader } from '@/components/common/Card';
import { DataTable, Column } from '@/components/tables/DataTable';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { ConfirmDialog } from '@/components/dialogs/Dialog';
import { mockDepartments, mockStudents, mockFaculty } from '@/services/mockData';
import { Department, Student, Faculty } from '@/types';
import { formatDate } from '@/utils';
import { cn } from '@/utils';
import toast from 'react-hot-toast';

const tabs = ['Departments', 'Students', 'Faculty'] as const;
type Tab = typeof tabs[number];

const deptColumns: Column<Department>[] = [
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

const studentColumns: Column<Student>[] = [
  { key: 'name', header: 'Name', sortable: true },
  { key: 'rollNumber', header: 'Roll No.', render: (v) => <span className="font-mono text-xs">{String(v)}</span> },
  { key: 'department', header: 'Department', sortable: true },
  { key: 'semester', header: 'Sem', align: 'center' },
  { key: 'cgpa', header: 'CGPA', align: 'center', sortable: true, render: (v) => <span className="font-semibold">{String(v)}</span> },
  {
    key: 'isActive',
    header: 'Status',
    align: 'center',
    render: (v) => <Badge variant={v ? 'success' : 'cancelled'}>{v ? 'Active' : 'Inactive'}</Badge>,
  },
];

const facultyColumns: Column<Faculty>[] = [
  { key: 'name', header: 'Name', sortable: true },
  { key: 'employeeId', header: 'Emp ID', render: (v) => <span className="font-mono text-xs">{String(v)}</span> },
  { key: 'designation', header: 'Designation', sortable: true },
  { key: 'department', header: 'Department', sortable: true },
  { key: 'isActive', header: 'Status', align: 'center', render: (v) => <Badge variant={v ? 'success' : 'cancelled'}>{v ? 'Active' : 'Inactive'}</Badge> },
];

export function AdminDashboard() {
  const [tab, setTab] = useState<Tab>('Departments');
  const [deleteOpen, setDeleteOpen] = useState(false);

  const totalStudents = mockDepartments.reduce((s, d) => s + d.studentCount, 0);
  const totalFaculty = mockDepartments.reduce((s, d) => s + d.facultyCount, 0);

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">Admin Dashboard</h1>
          <p className="text-muted-foreground text-sm mt-1">Manage departments, users, and system settings</p>
        </div>
        <Button variant="primary" leftIcon={<Plus className="h-4 w-4" />}>
          Add New
        </Button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard title="Departments" value={mockDepartments.length} icon={Building2} delay={0} />
        <StatCard title="Total Faculty" value={totalFaculty} icon={GraduationCap} iconBg="bg-secondary/10" iconColor="text-secondary" delay={0.05} />
        <StatCard title="Total Students" value={totalStudents} icon={Users} format="number" iconBg="bg-primary/10" iconColor="text-primary" delay={0.1} />
        <StatCard title="Active Exams" value={mockDepartments.reduce((s, d) => s + d.activeExams, 0)} icon={BarChart3} iconBg="bg-warning/10" iconColor="text-warning" delay={0.15} />
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
          data={mockDepartments}
          columns={deptColumns}
          keyField="id"
          searchable
          searchFields={['name', 'code', 'hodName']}
          actions={(dept) => (
            <>
              <Button variant="ghost" size="icon-sm" animate={false}><Edit className="h-3.5 w-3.5" /></Button>
              <Button variant="ghost" size="icon-sm" animate={false} onClick={() => setDeleteOpen(true)}><Trash2 className="h-3.5 w-3.5 text-danger" /></Button>
            </>
          )}
        />
      )}

      {tab === 'Students' && (
        <DataTable
          data={mockStudents}
          columns={studentColumns}
          keyField="id"
          searchable
          searchFields={['name', 'rollNumber', 'department']}
          actions={() => (
            <>
              <Button variant="ghost" size="icon-sm" animate={false}><Edit className="h-3.5 w-3.5" /></Button>
              <Button variant="ghost" size="icon-sm" animate={false} onClick={() => setDeleteOpen(true)}><Trash2 className="h-3.5 w-3.5 text-danger" /></Button>
            </>
          )}
        />
      )}

      {tab === 'Faculty' && (
        <DataTable
          data={mockFaculty}
          columns={facultyColumns}
          keyField="id"
          searchable
          searchFields={['name', 'employeeId', 'designation']}
          actions={() => (
            <>
              <Button variant="ghost" size="icon-sm" animate={false}><Edit className="h-3.5 w-3.5" /></Button>
              <Button variant="ghost" size="icon-sm" animate={false} onClick={() => setDeleteOpen(true)}><Trash2 className="h-3.5 w-3.5 text-danger" /></Button>
            </>
          )}
        />
      )}

      <ConfirmDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => { setDeleteOpen(false); toast.success('Deleted successfully'); }}
        title="Confirm Delete"
        description="Are you sure you want to delete this item? This action cannot be undone."
        confirmLabel="Delete"
        variant="danger"
      />
    </div>
  );
}
