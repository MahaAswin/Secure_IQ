import React, { useState } from 'react';
import { BarChart3, TrendingUp, Users, BookOpen, Filter } from 'lucide-react';
import { StatCard } from '@/components/cards/StatCard';
import { Card, CardHeader } from '@/components/common/Card';
import { AreaChartComponent, BarChartComponent, LineChartComponent, DonutChart } from '@/components/charts/Charts';
import { Select } from '@/components/forms/FormFields';
import { mockExamTrend, mockDeptPerformance, mockScoreDistribution } from '@/services/mockData';

const periods = [
  { value: 'month', label: 'This Month' },
  { value: 'semester', label: 'This Semester' },
  { value: 'year', label: 'This Year' },
];

export function AnalyticsPage() {
  const [period, setPeriod] = useState('semester');

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">Analytics</h1>
          <p className="text-muted-foreground text-sm mt-1">Comprehensive performance insights</p>
        </div>
        <Select
          options={periods}
          value={period}
          onChange={(e) => setPeriod(e.target.value)}
          className="w-40"
        />
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard title="Total Exams" value={42} icon={BookOpen} change={8.3} changeLabel="vs last period" delay={0} />
        <StatCard title="Students Appeared" value={1240} icon={Users} format="number" change={12.1} changeLabel="growth" iconBg="bg-secondary/10" iconColor="text-secondary" delay={0.05} />
        <StatCard title="Avg Score" value="74.8%" icon={TrendingUp} change={-1.2} changeLabel="vs last" iconBg="bg-primary/10" iconColor="text-primary" delay={0.1} />
        <StatCard title="Pass Rate" value="87%" icon={BarChart3} change={3.4} changeLabel="improved" iconBg="bg-success/10" iconColor="text-success" delay={0.15} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <Card>
          <CardHeader title="Exam Volume Trend" description="Number of exams conducted per month" icon={<BarChart3 className="h-4 w-4" />} />
          <AreaChartComponent
            data={mockExamTrend}
            areas={[
              { key: 'exams', label: 'Exams', color: '#2563EB' },
              { key: 'students', label: 'Students', color: '#7C3AED' },
            ]}
            xKey="name"
            height={220}
            showLegend
          />
        </Card>

        <Card>
          <CardHeader title="Score Distribution" description="Student score ranges across all exams" icon={<BarChart3 className="h-4 w-4" />} />
          <BarChartComponent
            data={mockScoreDistribution}
            bars={[{ key: 'value', label: 'Students', color: '#2563EB' }]}
            xKey="name"
            height={220}
          />
        </Card>

        <Card>
          <CardHeader title="Department Performance" description="Average scores by department" icon={<BarChart3 className="h-4 w-4" />} />
          <BarChartComponent
            data={mockDeptPerformance}
            bars={[
              { key: 'avgScore', label: 'Avg Score', color: '#2563EB' },
              { key: 'passRate', label: 'Pass Rate %', color: '#16A34A' },
            ]}
            xKey="name"
            height={220}
            showLegend
          />
        </Card>

        <Card>
          <CardHeader title="Monthly Score Trend" description="Average score progression over months" icon={<TrendingUp className="h-4 w-4" />} />
          <LineChartComponent
            data={mockExamTrend}
            lines={[{ key: 'avgScore', label: 'Avg Score', color: '#7C3AED' }]}
            xKey="name"
            height={220}
          />
        </Card>
      </div>

      {/* Grade distribution */}
      <Card>
        <CardHeader title="Grade Distribution Overview" description="Overall academic performance breakdown" icon={<BarChart3 className="h-4 w-4" />} />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <DonutChart
            data={[
              { name: 'Outstanding (O)', value: 10 },
              { name: 'Excellent (A+)', value: 15 },
              { name: 'Very Good (A)', value: 22 },
              { name: 'Good (B+)', value: 18 },
              { name: 'Average (B)', value: 12 },
              { name: 'Pass (C)', value: 8 },
              { name: 'Fail (F)', value: 5 },
            ]}
            height={280}
            centerLabel="Total Students"
            centerValue={1240}
          />
          <div className="flex flex-col justify-center space-y-3">
            {[
              { grade: 'O', label: 'Outstanding', count: 124, pct: 10, color: 'bg-blue-500' },
              { grade: 'A+', label: 'Excellent', count: 186, pct: 15, color: 'bg-purple-500' },
              { grade: 'A', label: 'Very Good', count: 273, pct: 22, color: 'bg-green-500' },
              { grade: 'B+', label: 'Good', count: 223, pct: 18, color: 'bg-amber-500' },
              { grade: 'B', label: 'Average', count: 149, pct: 12, color: 'bg-cyan-500' },
              { grade: 'C', label: 'Pass', count: 99, pct: 8, color: 'bg-pink-500' },
              { grade: 'F', label: 'Fail', count: 62, pct: 5, color: 'bg-red-500' },
            ].map((g) => (
              <div key={g.grade} className="flex items-center gap-3">
                <span className="w-8 text-xs font-bold text-right text-foreground">{g.grade}</span>
                <div className="flex-1 h-2 rounded-full bg-muted overflow-hidden">
                  <div className={`h-full rounded-full ${g.color}`} style={{ width: `${g.pct}%` }} />
                </div>
                <span className="text-xs text-muted-foreground w-20 text-right">{g.count} ({g.pct}%)</span>
              </div>
            ))}
          </div>
        </div>
      </Card>
    </div>
  );
}
