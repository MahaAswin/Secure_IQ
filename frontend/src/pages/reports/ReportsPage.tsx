import React from 'react';
import { FileText, Download, Eye, Filter } from 'lucide-react';
import { DataTable, Column } from '@/components/tables/DataTable';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { mockExams } from '@/services/mockData';
import { Exam, ExamStatus } from '@/types';
import { formatDate, formatDuration } from '@/utils';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants';

const columns: Column<Exam>[] = [
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
      const variantMap = { [ExamStatus.LIVE]: 'live', [ExamStatus.UPCOMING]: 'upcoming', [ExamStatus.COMPLETED]: 'completed', [ExamStatus.CANCELLED]: 'cancelled' } as const;
      return <Badge variant={variantMap[s]}>{s}</Badge>;
    },
  },
  { key: 'enrolledCount', header: 'Students', align: 'center', sortable: true },
];

export function ReportsPage() {
  const navigate = useNavigate();

  return (
    <div className="space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">Reports</h1>
          <p className="text-muted-foreground text-sm mt-1">View and export exam reports</p>
        </div>
        <Button variant="outline" leftIcon={<Download className="h-4 w-4" />}>Export All</Button>
      </div>

      <DataTable
        data={mockExams.filter((e) => e.status === ExamStatus.COMPLETED)}
        columns={columns}
        keyField="id"
        searchable
        searchFields={['title', 'code', 'facultyName']}
        emptyTitle="No completed exams"
        emptyDescription="Reports will appear once exams are completed."
        actions={(exam) => (
          <Button
            variant="ghost-primary"
            size="xs"
            leftIcon={<Eye className="h-3 w-3" />}
            onClick={() => navigate(ROUTES.AI_REPORT.replace(':examId', exam.id))}
            animate={false}
          >
            View Report
          </Button>
        )}
      />
    </div>
  );
}
