import React from 'react';
import { BarChart3, TrendingUp, Users, BookOpen, Award } from 'lucide-react';
import { StatCard } from '@/components/cards/StatCard';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { DataTable, Column } from '@/components/tables/DataTable';
import { BarChartComponent, AreaChartComponent, DonutChart } from '@/components/charts/Charts';
import { mockFaculty, mockDeptPerformance, mockExamTrend, mockScoreDistribution } from '@/services/mockData';
import { Faculty } from '@/types';
import { formatPercentage } from '@/utils';
import { useAuth } from '@/contexts/AuthContext';

const facultyColumns: Column<Faculty>[] = [
  {
    key: 'name',
    header: 'Faculty',
    sortable: true,
  },
  {
    key: 'designation',
    header: 'Designation',
    sortable: true,
  },
  {
    key: 'subjects',
    header: 'Subjects',
    render: (v) => {
      const subjects = v as string[];
      return (
        <div className="flex flex-wrap gap-1">
          {subjects.slice(0, 2).map((s) => (
            <Badge key={s} variant="primary" className="text-[10px]">{s}</Badge>
          ))}
          {subjects.length > 2 && <Badge variant="default">+{subjects.length - 2}</Badge>}
        </div>
      );
    },
  },
  {
    key: 'isActive',
    header: 'Status',
    align: 'center',
    render: (v) => <Badge variant={v ? 'success' : 'cancelled'}>{v ? 'Active' : 'Inactive'}</Badge>,
  },
];

export function HODDashboard() {
  const { user } = useAuth();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display font-bold text-foreground text-2xl">Department Overview</h1>
        <p className="text-muted-foreground text-sm mt-1">{user?.department ?? 'Computer Science'} Department</p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard title="Total Faculty" value={mockFaculty.length} icon={Users} delay={0} />
        <StatCard title="Active Exams" value={3} icon={BookOpen} iconBg="bg-primary/10" iconColor="text-primary" delay={0.05} />
        <StatCard title="Avg Score" value="76.2%" icon={TrendingUp} change={2.1} changeLabel="this semester" iconBg="bg-success/10" iconColor="text-success" delay={0.1} />
        <StatCard title="Pass Rate" value="88%" icon={Award} iconBg="bg-warning/10" iconColor="text-warning" delay={0.15} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-5">
          {/* Faculty table */}
          <Card>
            <CardHeader title="Faculty Performance" icon={<Users className="h-4 w-4" />} />
            <DataTable
              data={mockFaculty}
              columns={facultyColumns}
              keyField="id"
              searchable
              searchFields={['name', 'designation']}
              pageSize={5}
              emptyTitle="No faculty members"
            />
          </Card>

          {/* Trends */}
          <Card>
            <CardHeader title="Exam Trends" icon={<BarChart3 className="h-4 w-4" />} />
            <AreaChartComponent
              data={mockExamTrend}
              areas={[
                { key: 'exams', label: 'Exams', color: '#2563EB' },
                { key: 'avgScore', label: 'Avg Score', color: '#7C3AED' },
              ]}
              xKey="name"
              height={200}
              showLegend
            />
          </Card>
        </div>

        <div className="space-y-4">
          <Card>
            <CardHeader title="Score Distribution" icon={<BarChart3 className="h-4 w-4" />} />
            <DonutChart
              data={[
                { name: 'O (90-100)', value: 10 },
                { name: 'A+ (80-90)', value: 15 },
                { name: 'A (70-80)', value: 22 },
                { name: 'B (60-70)', value: 18 },
                { name: 'C (50-60)', value: 12 },
                { name: 'F (<50)', value: 5 },
              ]}
              centerLabel="Students"
              centerValue={82}
            />
          </Card>

          <Card>
            <CardHeader title="Dept. Comparison" icon={<BarChart3 className="h-4 w-4" />} />
            <BarChartComponent
              data={mockDeptPerformance}
              bars={[{ key: 'avgScore', label: 'Score' }]}
              xKey="name"
              height={150}
            />
          </Card>
        </div>
      </div>
    </div>
  );
}
