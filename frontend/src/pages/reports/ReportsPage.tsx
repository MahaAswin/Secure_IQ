import React, { useEffect, useState } from 'react';
import { Download, Eye, FileSpreadsheet, FileText, ChevronDown } from 'lucide-react';
import { DataTable, Column } from '@/components/tables/DataTable';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { ExamStatus } from '@/types';
import { formatDate, formatDuration, downloadCsv, downloadJson } from '@/utils';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants';
import api from '@/services/api';
import toast from 'react-hot-toast';

const mapStatus = (backendStatus: string): ExamStatus => {
  switch (backendStatus.toUpperCase()) {
    case 'ACTIVE':
      return ExamStatus.LIVE;
    case 'COMPLETED':
      return ExamStatus.COMPLETED;
    case 'CANCELLED':
      return ExamStatus.CANCELLED;
    case 'PUBLISHED':
    case 'DRAFT':
    default:
      return ExamStatus.UPCOMING;
  }
};

const columns: Column<any>[] = [
  { key: 'title', header: 'Exam', sortable: true },
  { key: 'code', header: 'Code', render: (v) => <span className="font-mono text-xs">{String(v)}</span> },
  { key: 'facultyName', header: 'Faculty', sortable: true },
  { key: 'startTime', header: 'Date', sortable: true, render: (v) => formatDate(String(v)) },
  { key: 'duration', header: 'Duration', render: (v) => formatDuration(Number(v)) },
  {
    key: 'status',
    header: 'Status',
    align: 'center',
    render: (v) => {
      const s = v as ExamStatus;
      const variantMap = {
        [ExamStatus.LIVE]: 'live',
        [ExamStatus.UPCOMING]: 'upcoming',
        [ExamStatus.COMPLETED]: 'completed',
        [ExamStatus.CANCELLED]: 'cancelled',
        [ExamStatus.AVAILABLE]: 'primary',
        [ExamStatus.IN_PROGRESS]: 'warning',
        [ExamStatus.EXPIRED]: 'cancelled',
      } as const;
      return <Badge variant={variantMap[s] || 'default'}>{s}</Badge>;
    },
  },
  { key: 'enrolledCount', header: 'Students', align: 'center', sortable: true },
];

export function ReportsPage() {
  const navigate = useNavigate();
  const [exams, setExams] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [showExportMenu, setShowExportMenu] = useState(false);

  useEffect(() => {
    async function loadReportsExams() {
      try {
        const res = await api.get('/v1/exams', { params: { size: 100 } });
        const backendExams = res.data.data.content || [];
        const mappedExams = backendExams.map((exam: any) => ({
          id: exam.id.toString(),
          title: exam.examTitle,
          facultyName: exam.faculty?.name || 'N/A',
          code: exam.examCode,
          startTime: `${exam.scheduledDate}T${exam.startTime}`,
          duration: exam.durationMinutes,
          status: mapStatus(exam.status),
          enrolledCount: 45, // Fallback representation
        }));
        setExams(mappedExams);
      } catch (err) {
        toast.error('Failed to load completed exams reports list');
      } finally {
        setLoading(false);
      }
    }
    loadReportsExams();
  }, []);

  const handleExportAllCsv = () => {
    if (!exams.length) {
      toast.error('No exam reports available to export');
      return;
    }

    setExporting(true);
    try {
      const todayStr = new Date().toISOString().split('T')[0];
      const headers = [
        'Exam Title',
        'Exam Code',
        'Faculty Member',
        'Scheduled Date',
        'Duration (Minutes)',
        'Exam Status',
        'Students Enrolled',
      ];

      const rows = exams.map((exam) => [
        exam.title || 'Untitled Exam',
        exam.code || '',
        exam.facultyName || 'N/A',
        exam.startTime ? formatDate(exam.startTime) : 'N/A',
        exam.duration || 0,
        exam.status || 'UPCOMING',
        exam.enrolledCount || 0,
      ]);

      downloadCsv(headers, rows, `SecureIQ_Exam_Reports_${todayStr}.csv`);
      toast.success(`Exported ${exams.length} exam reports to CSV`);
    } catch (err) {
      toast.error('Failed to export reports');
    } finally {
      setExporting(false);
      setShowExportMenu(false);
    }
  };

  const handleExportAllJson = () => {
    if (!exams.length) {
      toast.error('No exam reports available to export');
      return;
    }

    setExporting(true);
    try {
      const todayStr = new Date().toISOString().split('T')[0];
      downloadJson(exams, `SecureIQ_Exam_Reports_${todayStr}.json`);
      toast.success(`Exported ${exams.length} exam reports as JSON`);
    } catch (err) {
      toast.error('Failed to export reports');
    } finally {
      setExporting(false);
      setShowExportMenu(false);
    }
  };

  const handleExportSingle = (exam: any) => {
    try {
      const todayStr = new Date().toISOString().split('T')[0];
      const headers = ['Report Field', 'Details'];
      const rows = [
        ['Exam Title', exam.title || 'Untitled'],
        ['Exam Code', exam.code || 'N/A'],
        ['Faculty Member', exam.facultyName || 'N/A'],
        ['Date Scheduled', exam.startTime ? formatDate(exam.startTime) : 'N/A'],
        ['Duration', `${exam.duration || 0} minutes`],
        ['Status', exam.status || 'UPCOMING'],
        ['Enrolled Students', exam.enrolledCount || 0],
        ['Export Timestamp', new Date().toLocaleString()],
      ];

      const safeCode = (exam.code || exam.title || 'Report').replace(/[^a-zA-Z0-9_-]/g, '_');
      downloadCsv(headers, rows, `Exam_Report_${safeCode}_${todayStr}.csv`);
      toast.success(`Exported report for "${exam.title}"`);
    } catch (err) {
      toast.error('Failed to export single report');
    }
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">Reports</h1>
          <p className="text-muted-foreground text-sm mt-1">View and export exam reports</p>
        </div>

        {/* Export All Controls with Dropdown / Direct CSV action */}
        <div className="relative">
          <div className="flex items-center rounded-lg shadow-sm">
            <Button
              variant="outline"
              leftIcon={<Download className="h-4 w-4" />}
              onClick={handleExportAllCsv}
              isLoading={exporting}
              className="rounded-r-none border-r-0"
            >
              Export All
            </Button>
            <Button
              variant="outline"
              onClick={() => setShowExportMenu(!showExportMenu)}
              className="px-2 rounded-l-none"
              title="More export formats"
            >
              <ChevronDown className="h-4 w-4 text-muted-foreground" />
            </Button>
          </div>

          {showExportMenu && (
            <div className="absolute right-0 mt-2 w-48 rounded-xl border border-border bg-card p-1 shadow-elevated z-50">
              <button
                onClick={handleExportAllCsv}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-xs font-medium text-foreground hover:bg-accent transition-colors"
              >
                <FileSpreadsheet className="h-4 w-4 text-emerald-600" />
                Export as CSV (Excel)
              </button>
              <button
                onClick={handleExportAllJson}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-xs font-medium text-foreground hover:bg-accent transition-colors"
              >
                <FileText className="h-4 w-4 text-primary" />
                Export as JSON
              </button>
            </div>
          )}
        </div>
      </div>

      <DataTable
        data={exams}
        columns={columns}
        keyField="id"
        searchable
        searchFields={['title', 'code', 'facultyName']}
        emptyTitle="No completed exams"
        emptyDescription="Reports will appear once exams are completed."
        actions={(exam) => (
          <div className="flex items-center gap-2">
            <Button
              variant="ghost-primary"
              size="xs"
              leftIcon={<Eye className="h-3.5 w-3.5" />}
              onClick={() => navigate(ROUTES.AI_REPORT.replace(':examId', exam.id))}
              animate={false}
            >
              View Report
            </Button>
            <Button
              variant="ghost"
              size="xs"
              leftIcon={<Download className="h-3.5 w-3.5" />}
              onClick={() => handleExportSingle(exam)}
              title="Export report CSV"
              animate={false}
            >
              Export
            </Button>
          </div>
        )}
      />
    </div>
  );
}
