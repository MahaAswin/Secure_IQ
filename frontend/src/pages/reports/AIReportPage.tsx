import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Brain, AlertTriangle, CheckCircle, TrendingDown, Info, Download, FileText } from 'lucide-react';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { DataTable, Column } from '@/components/tables/DataTable';
import { DonutChart, BarChartComponent } from '@/components/charts/Charts';
import { formatDate, downloadCsv } from '@/utils';
import api from '@/services/api';
import toast from 'react-hot-toast';
import { ROUTES } from '@/constants';

const getGrade = (percentage: number) => {
  if (percentage >= 90) return 'O';
  if (percentage >= 80) return 'A+';
  if (percentage >= 70) return 'A';
  if (percentage >= 60) return 'B+';
  if (percentage >= 50) return 'B';
  if (percentage >= 40) return 'C';
  return 'F';
};

const resultColumns: Column<any>[] = [
  { key: 'studentName', header: 'Student', sortable: true },
  { key: 'obtainedMarks', header: 'Score', sortable: true, render: (v, row) => `${v}/${row.totalMarks}` },
  { key: 'percentage', header: '%', sortable: true, align: 'center', render: (v) => <span className="font-mono">{Number(v).toFixed(1)}%</span> },
  { key: 'grade', header: 'Grade', align: 'center', render: (v) => {
    const g = String(v);
    return <Badge variant={g === 'O' || g === 'A+' ? 'success' : g === 'F' ? 'danger' : 'warning'}>{g}</Badge>;
  }},
  {
    key: 'violationCount',
    header: 'AI Flags',
    align: 'center',
    render: (v) => {
      const flags = Number(v);
      return flags > 0 ? (
        <Badge variant="danger">{flags} flag{flags > 1 ? 's' : ''}</Badge>
      ) : (
        <span className="text-success-600 text-xs font-medium">Clean</span>
      );
    },
  },
];

export function AIReportPage() {
  const { examId } = useParams();
  const navigate = useNavigate();
  const [exam, setExam] = useState<any>(null);
  const [attempts, setAttempts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadReportDetails() {
      try {
        if (!examId) return;

        // 1. Fetch exam
        const examRes = await api.get(`/v1/exams/${examId}`);
        const backendExam = examRes.data.data;
        setExam(backendExam);

        // 2. Fetch session
        const sessionRes = await api.get('/v1/exam-sessions', {
          params: { examId: backendExam.id, size: 5 }
        });
        const sessions = sessionRes.data.data.content || [];
        const session = sessions[0];

        if (session) {
          // 3. Fetch attempts
          const attemptsRes = await api.get('/v1/attempts', {
            params: { examSessionId: session.id, size: 100 }
          });
          const fetchedAttempts = attemptsRes.data.data.content || [];

          setAttempts(fetchedAttempts.map((att: any) => {
            const obtained = att.obtainedMarks || 0;
            const total = backendExam.totalMarks || 100;
            const pct = (obtained / total) * 100;
            return {
              studentName: att.student?.user?.firstName
                ? `${att.student.user.firstName} ${att.student.user.lastName || ''}`.trim()
                : (att.student?.registerNumber || 'Student'),
              obtainedMarks: obtained,
              totalMarks: total,
              percentage: pct,
              grade: getGrade(pct),
              violationCount: att.totalViolations || 0
            };
          }));
        }
      } catch (err) {
        toast.error('Failed to load report analytics details');
      } finally {
        setLoading(false);
      }
    }
    loadReportDetails();
  }, [examId]);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  if (!exam) {
    return (
      <div className="p-6 text-center text-muted-foreground">
        Exam report not found.
      </div>
    );
  }

  const passed = attempts.filter((r) => r.percentage >= 40).length;
  const failed = attempts.filter((r) => r.percentage < 40).length;
  const avg = attempts.length ? attempts.reduce((s, r) => s + r.percentage, 0) / attempts.length : 0;

  // AI insights generator based on proctor flags
  const criticalFlagsCount = attempts.reduce((sum, r) => sum + r.violationCount, 0);
  const aiInsights = [
    {
      id: '1',
      severity: 'MEDIUM',
      category: 'Focus Active',
      affectedStudents: attempts.filter((a) => a.violationCount > 0).length,
      insight: `AI detected eye contact anomaly or looking away in ${attempts.filter((a) => a.violationCount > 0).length} student sessions.`,
      recommendation: 'Verify the proctor timeline flags to rule out external screen placement.'
    },
    {
      id: '2',
      severity: criticalFlagsCount > 5 ? 'HIGH' : 'LOW',
      category: 'Proctor Integrity',
      affectedStudents: attempts.filter((a) => a.violationCount > 2).length,
      insight: `${attempts.filter((a) => a.violationCount > 2).length} students logged more than 2 critical flags.`,
      recommendation: 'Recommend manual review of web feed logs for flagged students.'
    }
  ];

  const scoreDistribution = [
    { name: '0-40', value: failed },
    { name: '40-60', value: attempts.filter((a) => a.percentage >= 40 && a.percentage < 60).length },
    { name: '60-80', value: attempts.filter((a) => a.percentage >= 60 && a.percentage < 80).length },
    { name: '80-100', value: attempts.filter((a) => a.percentage >= 80).length },
  ];

  const handleExportPdf = () => {
    window.print();
  };

  const handleExportCsv = () => {
    if (!attempts.length) {
      toast.error('No candidate attempt records to export');
      return;
    }
    const todayStr = new Date().toISOString().split('T')[0];
    const headers = [
      'Student Name',
      'Obtained Marks',
      'Total Marks',
      'Percentage (%)',
      'Grade',
      'AI Flags Count',
    ];
    const rows = attempts.map((a) => [
      a.studentName || 'Student',
      a.obtainedMarks || 0,
      a.totalMarks || exam?.totalMarks || 100,
      a.percentage ? a.percentage.toFixed(1) : '0.0',
      a.grade || getGrade(a.percentage || 0),
      a.violationCount || 0,
    ]);

    const safeCode = (exam.examCode || 'EXAM').replace(/[^a-zA-Z0-9_-]/g, '_');
    downloadCsv(headers, rows, `Exam_AI_Report_${safeCode}_${todayStr}.csv`);
    toast.success('AI Report exported as CSV');
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Brain className="h-5 w-5 text-secondary" />
            <span className="text-sm font-medium text-secondary-600 dark:text-secondary-400">Explainable AI Report</span>
          </div>
          <h1 className="font-display font-bold text-foreground text-2xl">{exam.examTitle}</h1>
          <p className="text-muted-foreground text-sm mt-0.5">{exam.examCode} · Generated {formatDate(new Date().toISOString())}</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" leftIcon={<Download className="h-4 w-4" />} onClick={handleExportCsv}>
            Export CSV
          </Button>
          <Button variant="primary" leftIcon={<FileText className="h-4 w-4" />} onClick={handleExportPdf}>
            Export PDF / Print
          </Button>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: 'Total Appeared', value: attempts.length, color: 'text-foreground' },
          { label: 'Passed', value: passed, color: 'text-success-600' },
          { label: 'Failed', value: failed, color: 'text-danger-600' },
          { label: 'Avg Score', value: `${avg.toFixed(1)}%`, color: 'text-foreground' },
        ].map((s, i) => (
          <motion.div
            key={s.label}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.05 }}
            className="rounded-xl border border-border bg-card p-4 text-center shadow-soft"
          >
            <p className={`font-display font-bold text-2xl ${s.color}`}>{s.value}</p>
            <p className="text-xs text-muted-foreground mt-1">{s.label}</p>
          </motion.div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* AI Insights */}
        <div className="lg:col-span-2 space-y-4">
          <Card>
            <CardHeader title="AI Insights" description="Patterns and recommendations generated by AI" icon={<Brain className="h-4 w-4" />} />
            <div className="space-y-3">
              {aiInsights.map((insight, i) => (
                <motion.div
                  key={insight.id}
                  initial={{ opacity: 0, x: -8 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.08 }}
                  className="p-4 rounded-lg border border-border bg-muted/30"
                >
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div className="flex items-center gap-2">
                      <Badge variant={insight.severity === 'HIGH' ? 'danger' : insight.severity === 'MEDIUM' ? 'warning' : 'success'}>
                        {insight.severity}
                      </Badge>
                      <span className="text-xs text-muted-foreground">{insight.category}</span>
                    </div>
                    <span className="text-xs text-muted-foreground">{insight.affectedStudents} students</span>
                  </div>
                  <p className="text-sm text-foreground font-medium mb-1.5">{insight.insight}</p>
                  <div className="flex gap-2 text-xs text-muted-foreground">
                    <Info className="h-3.5 w-3.5 flex-shrink-0 mt-0.5" />
                    <span>{insight.recommendation}</span>
                  </div>
                </motion.div>
              ))}
            </div>
          </Card>

          {/* Results table */}
          <Card>
            <CardHeader title="Student Results" icon={<FileText className="h-4 w-4" />} />
            <DataTable
              data={attempts}
              columns={resultColumns}
              keyField="id"
              searchable
              searchFields={['studentName']}
              pageSize={5}
              emptyTitle="No results"
            />
          </Card>
        </div>

        {/* Charts */}
        <div className="space-y-4">
          <Card>
            <CardHeader title="Pass / Fail" icon={<CheckCircle className="h-4 w-4" />} />
            <DonutChart
              data={[
                { name: 'Passed', value: passed },
                { name: 'Failed', value: failed },
              ]}
              centerLabel="Appeared"
              centerValue={attempts.length}
              innerRadius={50}
              outerRadius={80}
            />
          </Card>

          <Card>
            <CardHeader title="Score Distribution" icon={<TrendingDown className="h-4 w-4" />} />
            <BarChartComponent
              data={scoreDistribution}
              bars={[{ key: 'value', label: 'Students' }]}
              xKey="name"
              height={180}
            />
          </Card>
        </div>
      </div>
    </div>
  );
}
