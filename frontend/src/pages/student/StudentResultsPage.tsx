import React, { useEffect, useState } from 'react';
import { Award, TrendingUp, CheckCircle } from 'lucide-react';
import { DataTable, Column } from '@/components/tables/DataTable';
import { Badge } from '@/components/common/Badge';
import { StatCard } from '@/components/cards/StatCard';
import { Card, CardHeader } from '@/components/common/Card';
import { DonutChart, AreaChartComponent } from '@/components/charts/Charts';
import { formatDate, formatDuration } from '@/utils';
import api from '@/services/api';
import toast from 'react-hot-toast';

const getGrade = (percentage: number) => {
  if (percentage >= 90) return 'O';
  if (percentage >= 80) return 'A+';
  if (percentage >= 70) return 'A';
  if (percentage >= 60) return 'B+';
  if (percentage >= 50) return 'B';
  return 'C';
};

const columns: Column<any>[] = [
  {
    key: 'examTitle',
    header: 'Exam',
    sortable: true,
    render: (_, row) => row.examSession?.exam?.examTitle || 'Exam Attempt',
  },
  {
    key: 'submittedTime',
    header: 'Date',
    sortable: true,
    render: (v, row) => formatDate(String(v || row.createdAt)),
  },
  {
    key: 'obtainedMarks',
    header: 'Score',
    sortable: true,
    render: (v, row) => (
      <span className="font-semibold">
        {Math.round(Number(v || 0))}/{row.totalMarks || 100}
      </span>
    ),
  },
  {
    key: 'percentage',
    header: '%',
    sortable: true,
    align: 'center',
    render: (v) => <span className="font-mono font-semibold">{Math.round(Number(v || 0))}%</span>,
  },
  {
    key: 'grade',
    header: 'Grade',
    align: 'center',
    render: (_, row) => {
      const g = getGrade(row.percentage || 0);
      const variant = g === 'O' || g === 'A+' ? 'success' : 'warning';
      return <Badge variant={variant}>{g}</Badge>;
    },
  },
  {
    key: 'rank',
    header: 'Rank',
    align: 'center',
    render: () => '—',
  },
  {
    key: 'timeTaken',
    header: 'Time Taken',
    render: (_, row) => {
      if (row.startTime && row.submittedTime) {
        const start = new Date(row.startTime).getTime();
        const end = new Date(row.submittedTime).getTime();
        const diffSeconds = Math.max(0, Math.floor((end - start) / 1000));
        return formatDuration(Math.floor(diffSeconds / 60));
      }
      return '—';
    },
  },
];

export function StudentResultsPage() {
  const [attempts, setAttempts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchResults() {
      try {
        const response = await api.get('/v1/attempts/student', { params: { size: 100 } });
        setAttempts(response.data.data.content || []);
      } catch (err) {
        toast.error('Failed to load results');
      } finally {
        setLoading(false);
      }
    }
    fetchResults();
  }, []);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  const completedAttempts = attempts.filter(
    (a: any) => a.status === 'SUBMITTED' || a.status === 'AUTO_SUBMITTED'
  );

  const avgScore = completedAttempts.length
    ? completedAttempts.reduce((sum: number, a: any) => sum + (a.percentage || 0), 0) / completedAttempts.length
    : 0;

  const best = completedAttempts.length
    ? Math.max(...completedAttempts.map((a: any) => a.percentage || 0))
    : 0;

  const passed = completedAttempts.filter((a: any) => (a.percentage || 0) >= 40).length;

  // Calculate score distribution
  const distributionMap = {
    '0-40': 0,
    '40-50': 0,
    '50-60': 0,
    '60-70': 0,
    '70-80': 0,
    '80-90': 0,
    '90-100': 0,
  };

  completedAttempts.forEach((a: any) => {
    const p = a.percentage || 0;
    if (p < 40) distributionMap['0-40']++;
    else if (p < 50) distributionMap['40-50']++;
    else if (p < 60) distributionMap['50-60']++;
    else if (p < 70) distributionMap['60-70']++;
    else if (p < 80) distributionMap['70-80']++;
    else if (p < 90) distributionMap['80-90']++;
    else distributionMap['90-100']++;
  });

  const donutData = Object.keys(distributionMap).map((range) => ({
    name: range,
    value: distributionMap[range as keyof typeof distributionMap],
  }));

  // Group completed attempts by month for the Score Trend chart
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const trendMap: Record<string, { sum: number; count: number }> = {};
  completedAttempts.forEach((a: any) => {
    const dateStr = a.submittedTime || a.createdAt;
    if (dateStr) {
      const date = new Date(dateStr);
      const monthName = months[date.getMonth()];
      if (!trendMap[monthName]) {
        trendMap[monthName] = { sum: 0, count: 0 };
      }
      trendMap[monthName].sum += (a.percentage || 0);
      trendMap[monthName].count += 1;
    }
  });

  const computedTrend = Object.keys(trendMap)
    .map((month) => ({
      name: month,
      avgScore: Math.round(trendMap[month].sum / trendMap[month].count),
    }))
    .sort((a, b) => months.indexOf(a.name) - months.indexOf(b.name));

  const chartData = computedTrend.length ? computedTrend : [
    { name: 'No Data', avgScore: 0 }
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display font-bold text-foreground text-2xl">My Results</h1>
        <p className="text-muted-foreground text-sm mt-1">Track your academic performance</p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard title="Avg Score" value={`${avgScore.toFixed(1)}%`} icon={TrendingUp} delay={0} />
        <StatCard title="Best Score" value={`${best.toFixed(0)}%`} icon={Award} iconBg="bg-warning/10" iconColor="text-warning" delay={0.05} />
        <StatCard title="Exams Passed" value={passed} icon={CheckCircle} iconBg="bg-success/10" iconColor="text-success" delay={0.1} />
        <StatCard title="Total Exams" value={completedAttempts.length} icon={Award} iconBg="bg-secondary/10" iconColor="text-secondary" delay={0.15} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <DataTable
            data={completedAttempts}
            columns={columns}
            keyField="id"
            searchable
            searchFields={['examSession.exam.examTitle']}
            emptyTitle="No results yet"
            emptyDescription="Complete an exam to see your results here."
          />
        </div>
        <div className="space-y-4">
          <Card>
            <CardHeader title="Score Distribution" icon={<Award className="h-4 w-4" />} />
            <DonutChart data={donutData} showLegend={false} centerLabel="Total" centerValue={completedAttempts.length} />
          </Card>
          <Card>
            <CardHeader title="Performance Trend" icon={<TrendingUp className="h-4 w-4" />} />
            <AreaChartComponent data={chartData} areas={[{ key: 'avgScore', label: 'Score' }]} xKey="name" height={140} />
          </Card>
        </div>
      </div>
    </div>
  );
}
