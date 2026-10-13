import { useCallback, useEffect, useRef, useState } from 'react';
import { api, ApiRequestError } from '../services/api';
import type { ScanDetail, ScanSummary } from '../types/api';

interface ScanPolling {
  /** Live summary while the scan runs, null before the first response. */
  summary: ScanSummary | null;
  /** Full report once the scan completes. */
  detail: ScanDetail | null;
  loading: boolean;
  error: string | null;
}

/**
 * Polls GET /api/scans/{id} while the backend reports a running status.
 * The backend exposes the real stage, so the UI never invents progress.
 */
export function useScanPolling(scanId: number | string | null): ScanPolling {
  const [summary, setSummary] = useState<ScanSummary | null>(null);
  const [detail, setDetail] = useState<ScanDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const timer = useRef<number | null>(null);

  const stop = useCallback(() => {
    if (timer.current !== null) {
      window.clearTimeout(timer.current);
      timer.current = null;
    }
  }, []);

  useEffect(() => {
    setSummary(null);
    setDetail(null);
    setError(null);

    if (scanId === null) {
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);

    const poll = async () => {
      try {
        const next = await api.getScan(scanId);
        if (cancelled) return;
        setSummary(next.scan);
        if (next.scan.status === 'COMPLETED' || next.scan.status === 'FAILED') {
          setDetail(next);
          setLoading(false);
          return;
        }
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiRequestError ? e.message : 'Unexpected error while loading the scan.';
        setError(message);
        setLoading(false);
        return;
      }
      timer.current = window.setTimeout(poll, 700);
    };

    void poll();
    return () => {
      cancelled = true;
      stop();
    };
  }, [scanId, stop]);

  return { summary, detail, loading, error };
}
