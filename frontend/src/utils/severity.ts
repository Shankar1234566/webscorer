import type { CheckStatus, Severity } from '../types/api';

export const SEVERITY_ORDER: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'];

export function severityRank(severity: Severity): number {
  return SEVERITY_ORDER.length - SEVERITY_ORDER.indexOf(severity);
}

const SEVERITY_CLASSES: Record<Severity, string> = {
  CRITICAL: 'bg-rose-500/15 text-rose-300 ring-rose-500/30',
  HIGH: 'bg-orange-500/15 text-orange-300 ring-orange-500/30',
  MEDIUM: 'bg-amber-500/15 text-amber-300 ring-amber-500/30',
  LOW: 'bg-sky-500/15 text-sky-300 ring-sky-500/30',
  INFO: 'bg-slate-500/15 text-slate-300 ring-slate-500/30',
};

export function severityClass(severity: Severity): string {
  return SEVERITY_CLASSES[severity] ?? SEVERITY_CLASSES.INFO;
}

export const STATUS_LABEL: Record<CheckStatus, string> = {
  PASS: 'Passed',
  FAIL: 'Issue',
  NOT_APPLICABLE: 'Not applicable',
  NOT_RUN: 'Not run',
};
