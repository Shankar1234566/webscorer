import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { CategoryScore, Finding, Severity } from '../../types/api';
import { SEVERITY_ORDER } from '../../utils/severity';

const SEVERITY_COLORS: Record<Severity, string> = {
  CRITICAL: '#fb7185',
  HIGH: '#fb923c',
  MEDIUM: '#fbbf24',
  LOW: '#38bdf8',
  INFO: '#94a3b8',
};

interface Props {
  categoryScores: CategoryScore[];
  findings: Finding[];
}

/**
 * Two honest charts: category scores (only categories with real data) and the
 * severity distribution of the issues that were actually found.
 */
export default function ScoreCharts({ categoryScores, findings }: Props) {
  const scored = categoryScores.filter((score) => score.score !== null);
  const unscored = categoryScores.filter((score) => score.score === null);
  const radarData = scored.map((score) => ({
    name: score.label,
    score: score.score ?? 0,
    fullMark: 100,
  }));

  const issues = findings.filter((finding) => finding.status === 'FAIL');
  const severityData = SEVERITY_ORDER.filter((severity) => severity !== 'INFO').map((severity) => ({
    severity,
    count: issues.filter((issue) => issue.severity === severity).length,
  }));
  const totalIssues = issues.length;

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <div className="card p-5">
        <h3 className="text-sm font-semibold text-white">Category scores</h3>
        <p className="mt-1 text-xs text-slate-500">
          Categories without enough evidence are left out instead of being drawn as zero.
        </p>
        <div className="mt-3 h-[240px]">
          {radarData.length === 0 ? (
            <p className="flex h-full items-center justify-center text-sm text-slate-500">
              No category had enough data to score.
            </p>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <RadarChart data={radarData} outerRadius="72%">
                <PolarGrid stroke="rgba(148,163,184,0.18)" />
                <PolarAngleAxis dataKey="name" tick={{ fill: '#94a3b8', fontSize: 11 }} />
                <PolarRadiusAxis domain={[0, 100]} tick={false} axisLine={false} />
                <Radar
                  dataKey="score"
                  stroke="#22d3ee"
                  fill="#22d3ee"
                  fillOpacity={0.25}
                  isAnimationActive
                />
                <RechartsTooltip
                  contentStyle={{
                    background: '#0a1226',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: 12,
                    fontSize: 12,
                  }}
                  formatter={(value: number | string) => [`${value}/100`, 'Score']}
                />
              </RadarChart>
            </ResponsiveContainer>
          )}
        </div>
        {unscored.length > 0 && (
          <p className="mt-2 text-xs text-amber-300/80">
            Not scored: {unscored.map((score) => score.label).join(', ')} (not enough data).
          </p>
        )}
      </div>

      <div className="card p-5">
        <h3 className="text-sm font-semibold text-white">Issues by severity</h3>
        <p className="mt-1 text-xs text-slate-500">
          {totalIssues === 0
            ? 'No failing checks were recorded for this scan.'
            : `${totalIssues} failing check${totalIssues === 1 ? '' : 's'}, grouped by how much they matter.`}
        </p>
        <div className="mt-3 h-[240px]">
          {totalIssues === 0 ? (
            <p className="flex h-full items-center justify-center text-sm text-emerald-300/80">
              Nothing to fix - every check that ran passed.
            </p>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={severityData} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.12)" />
                <XAxis dataKey="severity" tick={{ fill: '#94a3b8', fontSize: 11 }} />
                <YAxis allowDecimals={false} tick={{ fill: '#94a3b8', fontSize: 11 }} />
                <RechartsTooltip
                  cursor={{ fill: 'rgba(255,255,255,0.04)' }}
                  contentStyle={{
                    background: '#0a1226',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: 12,
                    fontSize: 12,
                  }}
                />
                <Bar dataKey="count" radius={[6, 6, 0, 0]} maxBarSize={56}>
                  {severityData.map((entry) => (
                    <Cell key={entry.severity} fill={SEVERITY_COLORS[entry.severity]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  );
}
