import React, { useState } from 'react';
import { BookOpen } from 'lucide-react';
import { ExamCard } from '@/components/cards/ExamCard';
import { SearchBar } from '@/components/common/SearchBar';
import { Badge } from '@/components/common/Badge';
import { EmptyState } from '@/components/common/EmptyState';
import { mockExams } from '@/services/mockData';
import { ExamStatus } from '@/types';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants';
import { cn } from '@/utils';

const STATUS_FILTERS = [
  { label: 'All', value: 'all' },
  { label: 'Upcoming', value: ExamStatus.UPCOMING },
  { label: 'Live', value: ExamStatus.LIVE },
  { label: 'Completed', value: ExamStatus.COMPLETED },
];

export function StudentExamsPage() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<string>('all');
  const [query, setQuery] = useState('');

  const filtered = mockExams.filter((exam) => {
    const matchStatus = filter === 'all' || exam.status === filter;
    const matchQuery = !query || exam.title.toLowerCase().includes(query.toLowerCase()) || exam.code.toLowerCase().includes(query.toLowerCase());
    return matchStatus && matchQuery;
  });

  return (
    <div className="space-y-5">
      <div>
        <h1 className="font-display font-bold text-foreground text-2xl">My Exams</h1>
        <p className="text-muted-foreground text-sm mt-1">All your scheduled and past examinations</p>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <SearchBar onSearch={setQuery} placeholder="Search exams..." className="flex-1 max-w-xs" />
        <div className="flex gap-2">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => setFilter(f.value)}
              className={cn(
                'px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                filter === f.value
                  ? 'bg-primary text-white'
                  : 'bg-muted text-muted-foreground hover:text-foreground'
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <EmptyState icon={BookOpen} title="No exams found" description="Try adjusting your search or filters." />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map((exam, i) => (
            <ExamCard
              key={exam.id}
              exam={exam}
              role="student"
              delay={i * 0.05}
              onJoin={() => navigate(ROUTES.STUDENT.JOIN_EXAM.replace(':examId', exam.id))}
              onView={() => navigate(ROUTES.STUDENT.RESULTS)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
