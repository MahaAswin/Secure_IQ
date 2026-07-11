import React, { useState, useMemo } from 'react';
import { ChevronUp, ChevronDown, ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/utils';
import { Button } from '@/components/common/Button';
import { SearchBar } from '@/components/common/SearchBar';
import { Skeleton } from '@/components/common/Skeleton';
import { EmptyState } from '@/components/common/EmptyState';
import { Database } from 'lucide-react';

export interface Column<T> {
  key: keyof T | string;
  header: string;
  sortable?: boolean;
  width?: string;
  align?: 'left' | 'center' | 'right';
  render?: (value: unknown, row: T) => React.ReactNode;
}

interface DataTableProps<T extends object> {
  data: T[];
  columns: Column<T>[];
  keyField: keyof T;
  searchable?: boolean;
  searchFields?: (keyof T)[];
  pageSize?: number;
  isLoading?: boolean;
  className?: string;
  emptyTitle?: string;
  emptyDescription?: string;
  actions?: (row: T) => React.ReactNode;
  onRowClick?: (row: T) => void;
}

type SortDir = 'asc' | 'desc' | null;

export function DataTable<T extends object>({
  data,
  columns,
  keyField,
  searchable = true,
  searchFields = [],
  pageSize = 10,
  isLoading = false,
  className,
  emptyTitle = 'No data found',
  emptyDescription,
  actions,
  onRowClick,
}: DataTableProps<T>) {
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDir, setSortDir] = useState<SortDir>(null);

  const filtered = useMemo(() => {
    if (!query) return data;
    const q = query.toLowerCase();
    return data.filter((row) => {
      const fields = searchFields.length > 0 ? searchFields : (Object.keys(row) as (keyof T)[]);
      return fields.some((f) => String(row[f]).toLowerCase().includes(q));
    });
  }, [data, query, searchFields]);

  const sorted = useMemo(() => {
    if (!sortKey || !sortDir) return filtered;
    return [...filtered].sort((a, b) => {
      const aVal = (a as Record<string, unknown>)[sortKey] as string | number;
      const bVal = (b as Record<string, unknown>)[sortKey] as string | number;
      if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filtered, sortKey, sortDir]);

  const totalPages = Math.ceil(sorted.length / pageSize);
  const paginated = sorted.slice((page - 1) * pageSize, page * pageSize);

  const handleSort = (key: string) => {
    if (sortKey !== key) {
      setSortKey(key);
      setSortDir('asc');
    } else if (sortDir === 'asc') {
      setSortDir('desc');
    } else {
      setSortKey(null);
      setSortDir(null);
    }
    setPage(1);
  };

  const handleSearch = (q: string) => {
    setQuery(q);
    setPage(1);
  };

  if (isLoading) {
    return (
      <div className={cn('rounded-xl border border-border bg-card overflow-hidden', className)}>
        <div className="p-4 border-b border-border">
          <div className="h-8 w-48 bg-muted rounded animate-pulse" />
        </div>
        <div className="p-4 space-y-3">
          {Array.from({ length: pageSize }).map((_, i) => (
            <div key={i} className="flex gap-4">
              {columns.map((_, j) => (
                <Skeleton key={j} className="h-5 flex-1" />
              ))}
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className={cn('rounded-xl border border-border bg-card overflow-hidden shadow-soft', className)}>
      {searchable && (
        <div className="px-4 py-3 border-b border-border flex items-center justify-between gap-3">
          <SearchBar onSearch={handleSearch} placeholder="Search..." className="max-w-xs w-full" />
          <span className="text-xs text-muted-foreground whitespace-nowrap">
            {sorted.length} result{sorted.length !== 1 ? 's' : ''}
          </span>
        </div>
      )}

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border bg-muted/40">
              {columns.map((col) => (
                <th
                  key={String(col.key)}
                  className={cn(
                    'px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wide whitespace-nowrap',
                    col.align === 'center' && 'text-center',
                    col.align === 'right' && 'text-right',
                    col.sortable && 'cursor-pointer hover:text-foreground transition-colors select-none',
                    col.width && `w-${col.width}`
                  )}
                  onClick={() => col.sortable && handleSort(String(col.key))}
                >
                  <span className="inline-flex items-center gap-1">
                    {col.header}
                    {col.sortable && (
                      <span className="flex flex-col">
                        <ChevronUp
                          className={cn(
                            'h-2.5 w-2.5 -mb-0.5',
                            sortKey === String(col.key) && sortDir === 'asc'
                              ? 'text-primary'
                              : 'text-muted-foreground/40'
                          )}
                        />
                        <ChevronDown
                          className={cn(
                            'h-2.5 w-2.5',
                            sortKey === String(col.key) && sortDir === 'desc'
                              ? 'text-primary'
                              : 'text-muted-foreground/40'
                          )}
                        />
                      </span>
                    )}
                  </span>
                </th>
              ))}
              {actions && <th className="px-4 py-3 text-right text-xs font-semibold text-muted-foreground uppercase">Actions</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {paginated.length === 0 ? (
              <tr>
                <td colSpan={columns.length + (actions ? 1 : 0)}>
                  <EmptyState
                    icon={Database}
                    title={emptyTitle}
                    description={emptyDescription}
                    size="sm"
                  />
                </td>
              </tr>
            ) : (
              paginated.map((row) => (
                <tr
                  key={String(row[keyField])}
                  className={cn(
                    'group transition-colors',
                    onRowClick && 'cursor-pointer hover:bg-muted/40'
                  )}
                  onClick={() => onRowClick?.(row)}
                >
                  {columns.map((col) => {
                    const rawVal = (row as Record<string, unknown>)[String(col.key)];
                    return (
                      <td
                        key={String(col.key)}
                        className={cn(
                          'px-4 py-3 text-sm text-foreground',
                          col.align === 'center' && 'text-center',
                          col.align === 'right' && 'text-right'
                        )}
                      >
                        {col.render ? col.render(rawVal, row) : String(rawVal ?? '')}
                      </td>
                    );
                  })}
                  {actions && (
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1" onClick={(e) => e.stopPropagation()}>
                        {actions(row)}
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="px-4 py-3 border-t border-border flex items-center justify-between gap-4">
          <span className="text-xs text-muted-foreground">
            Page {page} of {totalPages} · {sorted.length} total
          </span>
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="icon-sm"
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
              animate={false}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
              let pageNum: number;
              if (totalPages <= 5) pageNum = i + 1;
              else if (page <= 3) pageNum = i + 1;
              else if (page >= totalPages - 2) pageNum = totalPages - 4 + i;
              else pageNum = page - 2 + i;
              return (
                <button
                  key={pageNum}
                  onClick={() => setPage(pageNum)}
                  className={cn(
                    'h-7 w-7 rounded-md text-xs font-medium transition-colors',
                    page === pageNum
                      ? 'bg-primary text-white'
                      : 'text-muted-foreground hover:bg-muted'
                  )}
                >
                  {pageNum}
                </button>
              );
            })}
            <Button
              variant="ghost"
              size="icon-sm"
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
              animate={false}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
