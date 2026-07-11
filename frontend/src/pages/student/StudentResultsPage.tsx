import React from 'react';
import { Award, TrendingUp, CheckCircle } from 'lucide-react';
import { DataTable, Column } from '@/components/tables/DataTable';
import { Badge } from '@/components/common/Badge';
import { StatCard } from '@/components/cards/StatCard';
import { Card, CardHeader } from '@/components/common/Card';
import { DonutChart, AreaChartComponent } from '@/components/charts/Charts';
import { mockResults, mockExamTrend, mockScoreDistribution } from '@/services/mockData';
import { ExamResult } from '@/types';
import { formatDate, formatDuration } from '@/utils';

const columns: Column<ExamResult>[] = [
  {
    key: 'examTitle',
    header: 'Exam',
    sortable: true,
  },
  {
    key: 'submittedAt',
    header: 'Date',
    sortable: true,
    render: (v) => formatDate(String(v)),
  },
  {
    key: 'obtainedMarks',
    header: 'Score',
    sortable: true,
    render: (v, row) => (
      <span className="font-semibold">
        {String(v)}/{row.totalMarks}
      </span>
    ),
  },
  {
    key: 'percentage',
    header: '%',
    sortable: true,
    align: 'center',
    render: (v) => <span className="font-mono font-semibold">{String(v)}%</span>,
  },
  {
    key: 'grade',
    header: 'Grade',
    align: 'center',
    render: (v) => {
      const g = String(v);
      const variant = g === 'O' || g === 'A+' ? 'success' : g === 'F' ? 'danger' : 'warning';
      return <Badge variant={variant}>{g}</Badge>;
    },
  },
  {
    key: 'rank',
    header: 'Rank',
    align: 'center',
    render: (v) => v ? <span className="font-semibold text-primary">#{String(v)}</span> : '—',
  },
  {
    key: 'timeTaken',
    header: 'Time Taken',
    render: (v) => formatDuration(Math.floor(Number(v) / 60)),
  },
];

export function StudentResultsPage() {
  const avgScore = mockResults.length
    ? mockResults.reduce((s, r) => s + r.percentage, 0) / mockResults.length
    : 0;
  const best = Math.max(...mockResults.map((r) => r.percentage), 0);
  const passed = mockResults.filter((r) => r.percentage >= 40).length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display font-bold text-foreground text-2xl">My Results</h1>
        <p className="text-muted-foreground text-sm mt-1">Track your academic performance</p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard title="Avg Score" value={`${avgScore.toFixed(1)}%`} icon={TrendingUp} delay={0} />
        <StatCard title="Best Score" value={`${best}%`} icon={Award} iconBg="bg-warning/10" iconColor="text-warning" delay={0.05} />
        <StatCard title="Exams Passed" value={passed} icon={CheckCircle} iconBg="bg-success/10" iconColor="text-success" delay={0.1} />
        <StatCard title="Total Exams" value={mockResults.length} icon={Award} iconBg="bg-secondary/10" iconColor="text-secondary" delay={0.15} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <DataTable
            data={mockResults}
            columns={columns}
            keyField="id"
            searchable
            searchFields={['examTitle']}
            emptyTitle="No results yet"
            emptyDescription="Complete an exam to see your results here."
          />
        </div>
        <div className="space-y-4">
          <Card>
            <CardHeader title="Score Distribution" icon={<Award className="h-4 w-4" />} />
            <DonutChart data={mockScoreDistribution} showLegend={false} centerLabel="Total" centerValue={mockResults.length} />
          </Card>
          <Card>
            <CardHeader title="Performance Trend" icon={<TrendingUp className="h-4 w-4" />} />
            <AreaChartComponent data={mockExamTrend} areas={[{ key: 'avgScore', label: 'Score' }]} xKey="name" height={140} />
          </Card>
        </div>
      </div>
    </div>
  );
}
