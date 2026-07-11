import React from 'react';
import {
  AreaChart as ReAreaChart,
  Area,
  BarChart as ReBarChart,
  Bar,
  LineChart as ReLineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { CHART_COLORS } from '@/constants';
import { cn } from '@/utils';

// ─── Custom Tooltip ───────────────────────────────────────────────────────────

interface TooltipPayload {
  name: string;
  value: number;
  color: string;
}

const CustomTooltip = ({ active, payload, label }: { active?: boolean; payload?: TooltipPayload[]; label?: string }) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg border border-border bg-card px-3 py-2 shadow-elevated">
      {label && <p className="mb-1.5 text-xs font-semibold text-foreground">{label}</p>}
      {payload.map((entry, i) => (
        <div key={i} className="flex items-center gap-2 text-xs">
          <span className="h-2 w-2 rounded-full" style={{ backgroundColor: entry.color }} />
          <span className="text-muted-foreground">{entry.name}:</span>
          <span className="font-semibold text-foreground">{entry.value}</span>
        </div>
      ))}
    </div>
  );
};

// ─── Shared props ─────────────────────────────────────────────────────────────

interface ChartProps {
  data: Record<string, unknown>[];
  height?: number;
  className?: string;
  showGrid?: boolean;
  showLegend?: boolean;
}

// ─── Area Chart ───────────────────────────────────────────────────────────────

interface AreaChartProps extends ChartProps {
  areas: { key: string; label: string; color?: string }[];
  xKey: string;
}

export function AreaChartComponent({ data, areas, xKey, height = 250, className, showGrid = true, showLegend }: AreaChartProps) {
  return (
    <div className={cn('w-full', className)}>
      <ResponsiveContainer width="100%" height={height}>
        <ReAreaChart data={data} margin={{ top: 4, right: 4, bottom: 0, left: -10 }}>
          <defs>
            {areas.map((area, i) => {
              const color = area.color || CHART_COLORS[i % CHART_COLORS.length];
              return (
                <linearGradient key={area.key} id={`gradient-${area.key}`} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor={color} stopOpacity={0.25} />
                  <stop offset="95%" stopColor={color} stopOpacity={0.02} />
                </linearGradient>
              );
            })}
          </defs>
          {showGrid && <CartesianGrid strokeDasharray="3 3" stroke="var(--tw-border-opacity, #E2E8F0)" opacity={0.5} />}
          <XAxis dataKey={xKey} tick={{ fontSize: 11, fill: '#64748B' }} axisLine={false} tickLine={false} />
          <YAxis tick={{ fontSize: 11, fill: '#64748B' }} axisLine={false} tickLine={false} />
          <Tooltip content={<CustomTooltip />} />
          {showLegend && <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '12px' }} />}
          {areas.map((area, i) => {
            const color = area.color || CHART_COLORS[i % CHART_COLORS.length];
            return (
              <Area
                key={area.key}
                type="monotone"
                dataKey={area.key}
                name={area.label}
                stroke={color}
                strokeWidth={2}
                fill={`url(#gradient-${area.key})`}
                dot={false}
                activeDot={{ r: 4, strokeWidth: 0 }}
              />
            );
          })}
        </ReAreaChart>
      </ResponsiveContainer>
    </div>
  );
}

// ─── Bar Chart ────────────────────────────────────────────────────────────────

interface BarChartProps extends ChartProps {
  bars: { key: string; label: string; color?: string }[];
  xKey: string;
  layout?: 'vertical' | 'horizontal';
}

export function BarChartComponent({ data, bars, xKey, height = 250, className, showGrid = true, showLegend, layout = 'horizontal' }: BarChartProps) {
  return (
    <div className={cn('w-full', className)}>
      <ResponsiveContainer width="100%" height={height}>
        <ReBarChart data={data} layout={layout} margin={{ top: 4, right: 4, bottom: 0, left: layout === 'vertical' ? 60 : -10 }}>
          {showGrid && <CartesianGrid strokeDasharray="3 3" opacity={0.4} />}
          {layout === 'horizontal' ? (
            <>
              <XAxis dataKey={xKey} tick={{ fontSize: 11, fill: '#64748B' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: '#64748B' }} axisLine={false} tickLine={false} />
            </>
          ) : (
            <>
              <XAxis type="number" tick={{ fontSize: 11, fill: '#64748B' }} axisLine={false} tickLine={false} />
              <YAxis dataKey={xKey} type="category" tick={{ fontSize: 11, fill: '#64748B' }} axisLine={false} tickLine={false} />
            </>
          )}
          <Tooltip content={<CustomTooltip />} />
          {showLegend && <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '12px' }} />}
          {bars.map((bar, i) => (
            <Bar
              key={bar.key}
              dataKey={bar.key}
              name={bar.label}
              fill={bar.color || CHART_COLORS[i % CHART_COLORS.length]}
              radius={[4, 4, 0, 0]}
              maxBarSize={48}
            />
          ))}
        </ReBarChart>
      </ResponsiveContainer>
    </div>
  );
}

// ─── Line Chart ───────────────────────────────────────────────────────────────

interface LineChartProps extends ChartProps {
  lines: { key: string; label: string; color?: string }[];
  xKey: string;
}

export function LineChartComponent({ data, lines, xKey, height = 250, className, showGrid = true, showLegend }: LineChartProps) {
  return (
    <div className={cn('w-full', className)}>
      <ResponsiveContainer width="100%" height={height}>
        <ReLineChart data={data} margin={{ top: 4, right: 4, bottom: 0, left: -10 }}>
          {showGrid && <CartesianGrid strokeDasharray="3 3" opacity={0.4} />}
          <XAxis dataKey={xKey} tick={{ fontSize: 11, fill: '#64748B' }} axisLine={false} tickLine={false} />
          <YAxis tick={{ fontSize: 11, fill: '#64748B' }} axisLine={false} tickLine={false} />
          <Tooltip content={<CustomTooltip />} />
          {showLegend && <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '12px' }} />}
          {lines.map((line, i) => (
            <Line
              key={line.key}
              type="monotone"
              dataKey={line.key}
              name={line.label}
              stroke={line.color || CHART_COLORS[i % CHART_COLORS.length]}
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 4, strokeWidth: 0 }}
            />
          ))}
        </ReLineChart>
      </ResponsiveContainer>
    </div>
  );
}

// ─── Donut Chart ──────────────────────────────────────────────────────────────

interface DonutChartProps {
  data: { name: string; value: number }[];
  height?: number;
  className?: string;
  showLegend?: boolean;
  innerRadius?: number;
  outerRadius?: number;
  centerLabel?: string;
  centerValue?: string | number;
}

export function DonutChart({
  data,
  height = 220,
  className,
  showLegend = true,
  innerRadius = 55,
  outerRadius = 85,
  centerLabel,
  centerValue,
}: DonutChartProps) {
  return (
    <div className={cn('w-full', className)}>
      <ResponsiveContainer width="100%" height={height}>
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            innerRadius={innerRadius}
            outerRadius={outerRadius}
            dataKey="value"
            paddingAngle={2}
          >
            {data.map((_, i) => (
              <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
            ))}
          </Pie>
          <Tooltip content={<CustomTooltip />} />
          {showLegend && (
            <Legend
              formatter={(value) => <span className="text-xs text-muted-foreground">{value}</span>}
            />
          )}
          {centerLabel && centerValue && (
            <text x="50%" y="50%" textAnchor="middle" dominantBaseline="middle">
              <tspan x="50%" dy="-6" className="font-bold" fontSize="20" fill="var(--foreground)" fontWeight="700">
                {centerValue}
              </tspan>
              <tspan x="50%" dy="18" fontSize="11" fill="#64748B">
                {centerLabel}
              </tspan>
            </text>
          )}
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
