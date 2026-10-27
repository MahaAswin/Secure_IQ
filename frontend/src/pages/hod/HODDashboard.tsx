import React, { useEffect, useState, useMemo } from 'react';
import {
  BarChart3,
  TrendingUp,
  Users,
  BookOpen,
  Award,
  Sparkles,
  Building2,
  Calendar,
  Layers,
  GraduationCap,
  Activity,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { StatCard } from '@/components/cards/StatCard';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { DataTable, Column } from '@/components/tables/DataTable';
import { DonutChart, LineChartComponent } from '@/components/charts/Charts';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/services/api';
import toast from 'react-hot-toast';
import {
  HOD_KPI_SUMMARY,
  HOD_DEMO_FACULTY,
  HOD_SCORE_DISTRIBUTION,
  HOD_EXAM_TRENDS,
  HOD_SUBJECT_PERFORMANCE,
  HOD_RECENT_EXAMS,
  HOD_TOP_FACULTY_RANKING,
  DemoFaculty,
  RecentExamRecord,
} from '@/data/hodDemoData';

export function HODDashboard() {
  const { user } = useAuth();

  // Mode toggle: preserves live backend API while offering comprehensive demo analytics
  const [useDemoData, setUseDemoData] = useState<boolean>(true);
  const [selectedDepartment, setSelectedDepartment] = useState<string>(
    user?.department || 'Computer Science & Engineering'
  );

  // Live backend data states
  const [backendFaculties, setBackendFaculties] = useState<any[]>([]);
  const [backendActiveExamsCount, setBackendActiveExamsCount] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);

  // Faculty table filter state
  const [facultyStatusFilter, setFacultyStatusFilter] = useState<'ALL' | 'ACTIVE' | 'ON_LEAVE'>('ALL');

  useEffect(() => {
    async function fetchLiveHODData() {
      try {
        const [facultiesRes, examsRes] = await Promise.all([
          api.get('/v1/faculties', { params: { size: 100 } }),
          api.get('/v1/exams', { params: { status: 'ACTIVE', size: 1 } }),
        ]);
        setBackendFaculties(facultiesRes.data?.data?.content || []);
        setBackendActiveExamsCount(examsRes.data?.data?.totalElements || 0);
      } catch (err) {
        // If backend is unavailable or empty, smoothly keep fallback ready
        console.warn('Backend metrics fetch notice:', err);
      } finally {
        setLoading(false);
      }
    }
    fetchLiveHODData();
  }, []);

  // Filtered faculty dataset
  const displayedFaculty = useMemo(() => {
    if (!useDemoData) {
      return backendFaculties;
    }
    if (facultyStatusFilter === 'ALL') {
      return HOD_DEMO_FACULTY;
    }
    return HOD_DEMO_FACULTY.filter((f) => f.status === facultyStatusFilter);
  }, [useDemoData, backendFaculties, facultyStatusFilter]);

  // Dynamic KPI counts based on active mode
  const kpiData = useMemo(() => {
    if (!useDemoData && backendFaculties.length > 0) {
      return {
        totalFaculty: backendFaculties.length,
        facultySubtext: `${backendFaculties.filter((f) => f.user?.status === 'ACTIVE').length} active`,
        activeExams: backendActiveExamsCount,
        examsSubtext: 'From live database',
        avgScore: '76.2%',
        scoreChange: 2.1,
        passRate: '88.0%',
        passRateChange: 3.2,
      };
    }
    return {
      totalFaculty: HOD_KPI_SUMMARY.totalFaculty,
      facultySubtext: `${HOD_KPI_SUMMARY.activeFaculty} active • ${HOD_KPI_SUMMARY.onLeaveFaculty} on leave`,
      activeExams: HOD_KPI_SUMMARY.activeExams,
      examsSubtext: `${HOD_KPI_SUMMARY.liveExams} live • ${HOD_KPI_SUMMARY.scheduledExams} scheduled`,
      avgScore: `${HOD_KPI_SUMMARY.averageScore}%`,
      scoreChange: HOD_KPI_SUMMARY.scoreSemesterChange,
      passRate: `${HOD_KPI_SUMMARY.passRate}%`,
      passRateChange: HOD_KPI_SUMMARY.passRateSemesterChange,
    };
  }, [useDemoData, backendFaculties, backendActiveExamsCount]);

  // Faculty table columns
  const facultyColumns: Column<DemoFaculty | any>[] = [
    {
      key: 'name',
      header: 'Faculty Member',
      sortable: true,
      render: (_, row) => {
        const name = row.name || (row.user?.firstName ? `${row.user.firstName} ${row.user.lastName}` : row.employeeId);
        const empId = row.employeeId || 'ID-N/A';
        const initials = name
          .split(' ')
          .map((n: string) => n[0])
          .slice(0, 2)
          .join('')
          .toUpperCase();

        return (
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary font-bold text-xs flex-shrink-0">
              {initials || 'FC'}
            </div>
            <div>
              <p className="font-semibold text-foreground text-sm tracking-tight">{name}</p>
              <p className="text-xs text-muted-foreground">{empId}</p>
            </div>
          </div>
        );
      },
    },
    {
      key: 'designation',
      header: 'Designation',
      sortable: true,
      render: (v) => <span className="text-xs text-foreground font-medium">{String(v || 'Faculty')}</span>,
    },
    {
      key: 'subjects',
      header: 'Assigned Subjects',
      render: (v) => {
        const subjects = (v as any[]) || [];
        return (
          <div className="flex flex-wrap gap-1 max-w-xs">
            {subjects.map((s, idx) => (
              <Badge key={idx} variant="primary" className="text-[11px] font-normal py-0.5">
                {typeof s === 'string' ? s : s.subjectName}
              </Badge>
            ))}
          </div>
        );
      },
    },
    {
      key: 'examsConducted',
      header: 'Exams',
      sortable: true,
      align: 'center',
      render: (v) => <span className="font-semibold text-sm text-foreground">{v ?? 1}</span>,
    },
    {
      key: 'studentsEvaluated',
      header: 'Students Evaluated',
      sortable: true,
      align: 'center',
      render: (v) => (
        <span className="font-medium text-xs text-muted-foreground">
          {v ? `${v} students` : '—'}
        </span>
      ),
    },
    {
      key: 'averageScore',
      header: 'Avg Score',
      sortable: true,
      render: (v) => {
        const val = typeof v === 'number' ? v : 75.0;
        const colorClass =
          val >= 80 ? 'bg-success text-success' : val >= 75 ? 'bg-primary text-primary' : 'bg-warning text-warning';
        return (
          <div className="flex items-center gap-2 min-w-[110px]">
            <div className="w-16 bg-muted rounded-full h-1.5 overflow-hidden flex-shrink-0">
              <div
                className={`h-full rounded-full ${val >= 80 ? 'bg-success' : val >= 75 ? 'bg-primary' : 'bg-warning'}`}
                style={{ width: `${Math.min(100, Math.max(0, val))}%` }}
              />
            </div>
            <span className="font-semibold text-xs text-foreground">{val.toFixed(1)}%</span>
          </div>
        );
      },
    },
    {
      key: 'passRate',
      header: 'Pass Rate',
      sortable: true,
      align: 'center',
      render: (v) => {
        const val = typeof v === 'number' ? v : 85.0;
        const badgeVariant = val >= 90 ? 'success' : val >= 85 ? 'primary' : 'warning';
        return (
          <Badge variant={badgeVariant} className="font-semibold text-xs">
            {val.toFixed(1)}%
          </Badge>
        );
      },
    },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      align: 'center',
      render: (v, row) => {
        const active = v === 'ACTIVE' || v === 'Active' || row.user?.status === 'ACTIVE';
        return (
          <Badge variant={active ? 'success' : 'warning'} dot>
            {active ? 'Active' : 'On Leave'}
          </Badge>
        );
      },
    },
  ];

  // Recent Exam Activity columns
  const examColumns: Column<RecentExamRecord>[] = [
    {
      key: 'title',
      header: 'Examination Title',
      render: (_, row) => (
        <div>
          <p className="font-medium text-sm text-foreground">{row.title}</p>
          <p className="text-xs text-muted-foreground">{row.examCode} • {row.subject}</p>
        </div>
      ),
    },
    {
      key: 'faculty',
      header: 'Faculty In-Charge',
      render: (v) => <span className="text-xs font-medium text-foreground">{String(v)}</span>,
    },
    {
      key: 'date',
      header: 'Exam Date',
      render: (v) => (
        <span className="text-xs text-muted-foreground flex items-center gap-1.5">
          <Calendar className="h-3 w-3" />
          {String(v)}
        </span>
      ),
    },
    {
      key: 'studentsEnrolled',
      header: 'Evaluation Status',
      render: (_, row) => {
        const pct = row.studentsEnrolled > 0 ? Math.round((row.studentsEvaluated / row.studentsEnrolled) * 100) : 0;
        return (
          <div className="space-y-1 min-w-[120px]">
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>{row.studentsEvaluated} / {row.studentsEnrolled}</span>
              <span className="font-semibold text-foreground">{pct}%</span>
            </div>
            <div className="h-1.5 w-full bg-muted rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full ${pct === 100 ? 'bg-success' : pct > 0 ? 'bg-primary' : 'bg-muted-foreground/30'}`}
                style={{ width: `${pct}%` }}
              />
            </div>
          </div>
        );
      },
    },
    {
      key: 'status',
      header: 'Status',
      align: 'center',
      render: (v) => {
        switch (v) {
          case 'RESULTS_PUBLISHED':
            return <Badge variant="success">Results Published</Badge>;
          case 'COMPLETED':
            return <Badge variant="primary">Completed</Badge>;
          case 'IN_PROGRESS':
            return <Badge variant="warning" dot pulse>In Progress</Badge>;
          case 'SCHEDULED':
          default:
            return <Badge variant="default">Scheduled</Badge>;
        }
      },
    },
  ];

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-12">
      {/* ─────────────────────────────────────────────────────────────────────────
          1. Header & Department Controls
      ───────────────────────────────────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border/60 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="font-display font-bold text-foreground text-2xl tracking-tight">
              Department Performance Analytics
            </h1>
            <Badge variant="primary" className="hidden sm:inline-flex items-center gap-1">
              <Building2 className="h-3 w-3" />
              HOD Portal
            </Badge>
          </div>
          <p className="text-muted-foreground text-sm mt-1 flex items-center gap-2">
            <span>Academic Performance, Examination Metrics & Faculty Evaluation</span>
            <span className="text-border">•</span>
            <span className="text-foreground font-medium">{selectedDepartment}</span>
          </p>
        </div>

        {/* Department and Demo Data Toggle Controls */}
        <div className="flex flex-wrap items-center gap-2.5">
          <select
            value={selectedDepartment}
            onChange={(e) => setSelectedDepartment(e.target.value)}
            className="text-xs font-medium bg-card border border-border rounded-lg px-3 py-2 text-foreground focus:outline-none focus:ring-1 focus:ring-primary shadow-soft"
          >
            <option value="Computer Science & Engineering">Computer Science & Engineering</option>
            <option value="Electronics & Communication">Electronics & Communication</option>
            <option value="Information Technology">Information Technology</option>
          </select>

          <Button
            variant={useDemoData ? 'primary' : 'outline'}
            size="sm"
            onClick={() => {
              setUseDemoData(!useDemoData);
              toast.success(
                useDemoData
                  ? 'Switched to Live Backend Data View'
                  : 'Switched to Enhanced Demonstration Analytics View'
              );
            }}
            className="text-xs gap-1.5 shadow-soft"
          >
            <Sparkles className="h-3.5 w-3.5" />
            {useDemoData ? 'Demo Analytics Active' : 'Switch to Demo Mode'}
          </Button>
        </div>
      </div>

      {/* Demo Analytics Banner Indicator */}
      {useDemoData && (
        <div className="rounded-xl border border-primary/20 bg-primary/5 px-4 py-3 flex items-center justify-between text-xs text-primary-900 dark:text-primary-200">
          <div className="flex items-center gap-2.5">
            <span className="flex h-2 w-2 rounded-full bg-primary animate-pulse" />
            <span className="font-semibold">Enterprise Demo Analytics Mode:</span>
            <span>Displaying comprehensive 24-faculty departmental analytics and interconnected grading data.</span>
          </div>
          <span className="text-[11px] text-muted-foreground font-mono hidden md:inline-block">
            PostgreSQL Connection Preserved
          </span>
        </div>
      )}

      {/* ─────────────────────────────────────────────────────────────────────────
          2. Summary KPI Cards
      ───────────────────────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard
          title="Total Faculty"
          value={kpiData.totalFaculty}
          icon={Users}
          subtext={kpiData.facultySubtext}
          iconBg="bg-blue-500/10"
          iconColor="text-blue-600 dark:text-blue-400"
          delay={0}
        />
        <StatCard
          title="Active Exams"
          value={kpiData.activeExams}
          icon={BookOpen}
          subtext={kpiData.examsSubtext}
          iconBg="bg-purple-500/10"
          iconColor="text-purple-600 dark:text-purple-400"
          delay={0.05}
        />
        <StatCard
          title="Average Score"
          value={kpiData.avgScore}
          change={kpiData.scoreChange}
          changeLabel="vs last semester"
          subtext="Evaluated across 1,200 exams"
          icon={TrendingUp}
          iconBg="bg-emerald-500/10"
          iconColor="text-emerald-600 dark:text-emerald-400"
          delay={0.1}
        />
        <StatCard
          title="Pass Rate"
          value={kpiData.passRate}
          change={kpiData.passRateChange}
          changeLabel="vs last semester"
          subtext="Department benchmark: 85%"
          icon={Award}
          iconBg="bg-amber-500/10"
          iconColor="text-amber-600 dark:text-amber-400"
          delay={0.15}
        />
      </div>

      {/* ─────────────────────────────────────────────────────────────────────────
          3. Faculty Performance Table & Score Distribution Donut Chart
      ───────────────────────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Left: Faculty Performance Table (2 Columns) */}
        <div className="lg:col-span-2 space-y-6">
          <Card padding="none" className="overflow-hidden">
            <div className="p-5 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <CardHeader
                title="Faculty Performance"
                description="Comparative metrics across examinations conducted and student performance"
                icon={<Users className="h-4 w-4" />}
                className="mb-0"
              />

              {/* Status Filter Tabs */}
              <div className="flex items-center gap-1 bg-muted/60 p-1 rounded-lg self-start sm:self-auto text-xs">
                {(['ALL', 'ACTIVE', 'ON_LEAVE'] as const).map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setFacultyStatusFilter(tab)}
                    className={`px-2.5 py-1 rounded-md font-medium transition-all ${
                      facultyStatusFilter === tab
                        ? 'bg-card text-foreground shadow-sm'
                        : 'text-muted-foreground hover:text-foreground'
                    }`}
                  >
                    {tab === 'ALL' ? 'All (8)' : tab === 'ACTIVE' ? 'Active (7)' : 'On Leave (1)'}
                  </button>
                ))}
              </div>
            </div>

            <div className="p-5">
              <DataTable
                data={displayedFaculty}
                columns={facultyColumns}
                keyField="id"
                searchable
                searchFields={['name', 'employeeId', 'designation']}
                pageSize={5}
                emptyTitle="No faculty members found"
                emptyDescription="Try clearing filters or search terms."
              />
            </div>
          </Card>

          {/* ───────────────────────────────────────────────────────────────────
              4. Exam Trends Line Chart
          ─────────────────────────────────────────────────────────────────── */}
          <Card>
            <div className="flex items-center justify-between mb-4">
              <CardHeader
                title="Exam Trends (6 Months)"
                description="Historical trends in exam count, department average score, and pass percentage"
                icon={<BarChart3 className="h-4 w-4" />}
                className="mb-0"
              />
              <div className="flex items-center gap-3 text-xs text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-blue-600" /> Avg Score %
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-emerald-600" /> Pass Rate %
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-purple-600" /> Exams
                </span>
              </div>
            </div>

            <LineChartComponent
              data={HOD_EXAM_TRENDS}
              lines={[
                { key: 'avgScore', label: 'Average Score (%)', color: '#2563EB' },
                { key: 'passRate', label: 'Pass Rate (%)', color: '#16A34A' },
                { key: 'exams', label: 'Exams Conducted', color: '#7C3AED' },
              ]}
              xKey="name"
              height={250}
              showLegend={false}
            />

            <div className="mt-4 pt-3 border-t border-border grid grid-cols-3 gap-2 text-center text-xs">
              <div className="p-2 rounded-lg bg-muted/40">
                <p className="text-muted-foreground">Total Exams (6 Mo)</p>
                <p className="text-base font-bold text-foreground mt-0.5">32 Exams</p>
              </div>
              <div className="p-2 rounded-lg bg-muted/40">
                <p className="text-muted-foreground">Peak Monthly Pass Rate</p>
                <p className="text-base font-bold text-emerald-600 dark:text-emerald-400 mt-0.5">88.0% (Apr)</p>
              </div>
              <div className="p-2 rounded-lg bg-muted/40">
                <p className="text-muted-foreground">Avg Semester Score Gain</p>
                <p className="text-base font-bold text-blue-600 dark:text-blue-400 mt-0.5">+2.7% Overall</p>
              </div>
            </div>
          </Card>
        </div>

        {/* Right: Score Distribution Donut Chart & Breakdown (1 Column) */}
        <div className="space-y-6">
          <Card padding="md">
            <CardHeader
              title="Score Distribution"
              description="Evaluated distribution across grading tiers"
              icon={<Award className="h-4 w-4" />}
            />

            {/* Donut Chart with center value */}
            <div className="py-2">
              <DonutChart
                data={HOD_SCORE_DISTRIBUTION.map((item) => ({
                  name: item.shortName,
                  value: item.value,
                  color: item.color,
                }))}
                showLegend={false}
                centerLabel="Students"
                centerValue="1,200"
                height={200}
                innerRadius={55}
                outerRadius={80}
              />
            </div>

            {/* Detailed Legend and Count Breakdown */}
            <div className="space-y-3 mt-4 pt-4 border-t border-border">
              {HOD_SCORE_DISTRIBUTION.map((item) => (
                <div
                  key={item.shortName}
                  className="flex items-center justify-between text-xs p-2 rounded-lg bg-muted/30 hover:bg-muted/50 transition-colors"
                >
                  <div className="flex items-center gap-2.5">
                    <span
                      className="h-2.5 w-2.5 rounded-full flex-shrink-0"
                      style={{ backgroundColor: item.color }}
                    />
                    <div>
                      <p className="font-semibold text-foreground">{item.shortName}</p>
                      <p className="text-[11px] text-muted-foreground">{item.range}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="font-bold text-foreground">{item.percentage}%</p>
                    <p className="text-[11px] text-muted-foreground">{item.value} students</p>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-4 p-3 rounded-xl bg-primary/5 border border-primary/10 text-xs text-muted-foreground flex items-center justify-between">
              <span>Total Evaluated Students:</span>
              <span className="font-bold text-foreground">1,200 (100%)</span>
            </div>
          </Card>

          {/* Quick Department Action Summary */}
          <Card padding="md" className="space-y-3">
            <CardHeader
              title="Performance Insights"
              description="Automated HOD academic highlights"
              icon={<Sparkles className="h-4 w-4" />}
            />
            <div className="space-y-2.5 text-xs">
              <div className="flex items-start gap-2.5 p-2.5 rounded-lg bg-emerald-500/10 text-emerald-800 dark:text-emerald-300">
                <CheckCircle2 className="h-4 w-4 flex-shrink-0 mt-0.5 text-emerald-600 dark:text-emerald-400" />
                <div>
                  <p className="font-semibold">High Performing Subjects</p>
                  <p className="text-[11px] opacity-90 mt-0.5">
                    Artificial Intelligence and Algorithms lead with average student scores &gt; 81%.
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-2.5 p-2.5 rounded-lg bg-amber-500/10 text-amber-800 dark:text-amber-300">
                <AlertCircle className="h-4 w-4 flex-shrink-0 mt-0.5 text-amber-600 dark:text-amber-400" />
                <div>
                  <p className="font-semibold">Attention Recommended</p>
                  <p className="text-[11px] opacity-90 mt-0.5">
                    Cyber Security pass rate is 82.0% with faculty currently on leave; remedial sessions recommended.
                  </p>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* ─────────────────────────────────────────────────────────────────────────
          5. Faculty Performance Rankings & Subject-Wise Performance
      ───────────────────────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
        {/* Top 5 Faculty Leaderboard */}
        <Card>
          <CardHeader
            title="Top Faculty Performance Rankings"
            description="Leading educators ranked by average student achievement and course outcomes"
            icon={<Award className="h-4 w-4 text-amber-500" />}
          />

          <div className="space-y-3">
            {HOD_TOP_FACULTY_RANKING.map((fac) => {
              const rankColor =
                fac.rank === 1
                  ? 'bg-amber-500 text-white shadow-sm'
                  : fac.rank === 2
                  ? 'bg-slate-400 text-white'
                  : fac.rank === 3
                  ? 'bg-amber-700 text-white'
                  : 'bg-muted text-muted-foreground';

              return (
                <div
                  key={fac.name}
                  className="flex items-center justify-between p-3.5 rounded-xl border border-border/80 bg-card hover:bg-muted/30 transition-colors gap-3"
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={`h-7 w-7 rounded-lg flex items-center justify-center font-bold text-xs flex-shrink-0 ${rankColor}`}
                    >
                      #{fac.rank}
                    </div>
                    <div>
                      <p className="font-semibold text-foreground text-sm">{fac.name}</p>
                      <p className="text-xs text-muted-foreground">
                        {fac.designation} • {fac.examsConducted} exams ({fac.studentsEvaluated} students)
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-4 text-right flex-shrink-0">
                    <div>
                      <p className="text-xs text-muted-foreground">Avg Score</p>
                      <p className="font-bold text-foreground text-sm text-primary">{fac.averageScore}%</p>
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground">Pass Rate</p>
                      <Badge variant="success" className="text-xs font-semibold py-0.5">
                        {fac.passRate}%
                      </Badge>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </Card>

        {/* Subject-Wise Performance Overview */}
        <Card>
          <CardHeader
            title="Subject-Wise Curriculum Performance"
            description="Overview of student outcomes across departmental courses"
            icon={<GraduationCap className="h-4 w-4" />}
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-[380px] overflow-y-auto pr-1">
            {HOD_SUBJECT_PERFORMANCE.map((sub) => {
              const isHigh = sub.averageScore >= 80;
              const isModerate = sub.averageScore >= 74;

              return (
                <div
                  key={sub.id}
                  className="p-3.5 rounded-xl border border-border bg-card/60 hover:bg-muted/40 transition-colors space-y-2.5"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-semibold text-xs text-foreground line-clamp-1">{sub.subjectName}</p>
                      <p className="text-[11px] text-muted-foreground">{sub.subjectCode} • {sub.facultyInCharge}</p>
                    </div>
                    <Badge
                      variant={isHigh ? 'success' : isModerate ? 'primary' : 'warning'}
                      className="text-[11px] font-semibold flex-shrink-0"
                    >
                      {sub.passRate}% pass
                    </Badge>
                  </div>

                  <div className="space-y-1">
                    <div className="flex justify-between text-[11px] text-muted-foreground">
                      <span>Evaluated: {sub.studentsEvaluated}</span>
                      <span className="font-semibold text-foreground">{sub.averageScore}% avg</span>
                    </div>
                    <div className="h-1.5 w-full bg-muted rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full ${
                          isHigh ? 'bg-success' : isModerate ? 'bg-primary' : 'bg-warning'
                        }`}
                        style={{ width: `${sub.averageScore}%` }}
                      />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </Card>
      </div>

      {/* ─────────────────────────────────────────────────────────────────────────
          6. Recent Examination Activity & Faculty Workload Overview
      ───────────────────────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Recent Exam Activity (2 Columns) */}
        <div className="lg:col-span-2">
          <Card padding="none" className="overflow-hidden">
            <div className="p-5 border-b border-border">
              <CardHeader
                title="Recent Department Examination Activity"
                description="Live and recently completed examination records across semester courses"
                icon={<Activity className="h-4 w-4" />}
                className="mb-0"
              />
            </div>
            <div className="p-5">
              <DataTable
                data={HOD_RECENT_EXAMS}
                columns={examColumns}
                keyField="id"
                searchable={false}
                pageSize={6}
              />
            </div>
          </Card>
        </div>

        {/* Faculty Workload Overview (1 Column) */}
        <div>
          <Card padding="md">
            <CardHeader
              title="Faculty Workload Overview"
              description="Evaluation and invigilation distribution"
              icon={<Layers className="h-4 w-4" />}
            />

            <div className="space-y-3">
              {HOD_DEMO_FACULTY.slice(0, 6).map((fac) => {
                const isHigh = fac.workloadCategory === 'High';
                const isLow = fac.workloadCategory === 'Low';
                const capacity = Math.min(100, Math.round((fac.studentsAssigned / 220) * 100));

                return (
                  <div
                    key={fac.id}
                    className="p-3 rounded-xl border border-border/70 bg-card hover:bg-muted/30 transition-colors space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <div className="min-w-0 flex-1">
                        <p className="font-semibold text-xs text-foreground truncate">{fac.name}</p>
                        <p className="text-[11px] text-muted-foreground truncate">{fac.designation}</p>
                      </div>
                      <Badge
                        variant={isHigh ? 'warning' : isLow ? 'default' : 'success'}
                        className="text-[10px] uppercase tracking-wider flex-shrink-0"
                      >
                        {fac.workloadCategory}
                      </Badge>
                    </div>

                    <div className="space-y-1">
                      <div className="flex justify-between text-[11px] text-muted-foreground">
                        <span>{fac.examsAssigned} exams assigned</span>
                        <span className="font-medium text-foreground">{fac.studentsAssigned} students</span>
                      </div>
                      <div className="h-1.5 w-full bg-muted rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full ${
                            isHigh ? 'bg-warning' : isLow ? 'bg-muted-foreground/40' : 'bg-primary'
                          }`}
                          style={{ width: `${capacity}%` }}
                        />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="mt-4 pt-3 border-t border-border flex items-center justify-between text-xs text-muted-foreground">
              <span>Workload Policy:</span>
              <span className="font-medium text-foreground">Max 220 students / faculty</span>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
