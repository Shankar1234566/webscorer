/** Small formatting helpers shared across pages. */

export function formatScore(score: number | null | undefined): string {
  return score === null || score === undefined ? '—' : String(score);
}

export function scoreBand(score: number | null): { label: string; tone: 'good' | 'ok' | 'weak' } {
  if (score === null) return { label: 'Not enough data', tone: 'ok' };
  if (score >= 85) return { label: 'Excellent', tone: 'good' };
  if (score >= 70) return { label: 'Good', tone: 'good' };
  if (score >= 50) return { label: 'Needs work', tone: 'ok' };
  return { label: 'Weak', tone: 'weak' };
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function hostOf(url: string): string {
  try {
    return new URL(url).host;
  } catch {
    return url;
  }
}

export function deltaLabel(current: number | null, previous: number | null): string | null {
  if (current === null || previous === null) return null;
  const diff = current - previous;
  if (diff === 0) return 'No change vs previous scan';
  return `${diff > 0 ? '+' : ''}${diff} vs previous scan`;
}
