import { ArrowRight, Globe, Inbox, Trash2 } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import EmptyState from '../components/ui/EmptyState';
import Modal from '../components/ui/Modal';
import Section from '../components/ui/Section';
import Button from '../components/ui/Button';
import { SkeletonCard } from '../components/ui/Skeleton';
import { useApp } from '../context/AppContext';
import { api, ApiRequestError } from '../services/api';
import type { ScanSummary } from '../types/api';
import { formatDate, hostOf, scoreBand } from '../utils/format';

const STATUS_STYLES: Record<string, string> = {
  COMPLETED: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  FAILED: 'bg-rose-500/15 text-rose-300 ring-rose-500/30',
};

export default function HistoryPage() {
  const [scans, setScans] = useState<ScanSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<ScanSummary | null>(null);
  const { pushToast, setScanDetail } = useApp();
  const navigate = useNavigate();

  const load = useCallback(() => {
    setLoading(true);
    api
      .listScans()
      .then((data) => {
        setScans(data);
        setError(null);
      })
      .catch((e) => setError(e instanceof ApiRequestError ? e.message : 'Could not load scan history.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(load, [load]);

  const confirmDelete = async () => {
    if (!pendingDelete) return;
    try {
      await api.deleteScan(pendingDelete.id);
      setScans((current) => current.filter((scan) => scan.id !== pendingDelete.id));
      setScanDetail(null);
      pushToast(`Report #${pendingDelete.id} deleted.`, 'success');
    } catch (e) {
      pushToast(e instanceof ApiRequestError ? e.message : 'Could not delete the report.', 'error');
    } finally {
      setPendingDelete(null);
    }
  };

  const completed = scans.filter((scan) => scan.status === 'COMPLETED');
  const compareCandidates = completed.filter((scan) => scan.overallScore !== null);

  return (
    <div className="space-y-6">
      <Section>
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-white">Scan History</h1>
            <p className="mt-2 max-w-2xl text-sm leading-relaxed text-slate-400">
              Every scan is stored in the local H2 database, so you can re-open a report or compare
              a URL against its previous score.
            </p>
          </div>
          <Button onClick={() => navigate('/scanner')}>
            New scan <ArrowRight className="h-4 w-4" />
          </Button>
        </div>
      </Section>

      {compareCandidates.length >= 2 && (
        <div className="rounded-xl border border-cyanx-400/25 bg-cyanx-500/10 px-4 py-3 text-sm text-cyanx-100">
          You have {compareCandidates.length} scored reports. Open one to see its previous score
          comparison in the report header.
        </div>
      )}

      {loading && (
        <div className="grid gap-4 md:grid-cols-2">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      )}

      {!loading && error && (
        <div className="card p-6">
          <p className="text-sm text-rose-300">{error}</p>
          <Button variant="secondary" size="sm" className="mt-3" onClick={load}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && scans.length === 0 && (
        <EmptyState
          icon={<Inbox className="h-6 w-6" />}
          title="No scans yet"
          description="Scan a website and the report will show up here with its score, findings and copilot conversation."
          action={
            <Link
              to="/scanner"
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-electric-500 to-violetish-600 px-4 py-2.5 text-sm font-medium text-white"
            >
              Scan your first website <ArrowRight className="h-4 w-4" />
            </Link>
          }
        />
      )}

      {!loading && !error && scans.length > 0 && (
        <div className="card overflow-hidden">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-white/[0.07] bg-white/[0.03] text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th className="px-5 py-3 font-medium">Website</th>
                <th className="px-5 py-3 font-medium">Scanned</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 font-medium">Score</th>
                <th className="px-5 py-3 font-medium">Issues</th>
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.05]">
              {scans.map((scan) => {
                const band = scoreBand(scan.overallScore);
                return (
                  <tr key={scan.id} className="transition hover:bg-white/[0.02]">
                    <td className="px-5 py-3.5">
                      <Link to={`/report/${scan.id}`} className="flex items-center gap-2.5">
                        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/[0.05] text-slate-400">
                          <Globe className="h-4 w-4" />
                        </span>
                        <span className="min-w-0">
                          <span className="block truncate font-medium text-slate-200">
                            {hostOf(scan.url)}
                          </span>
                          <span className="block truncate text-xs text-slate-500">{scan.url}</span>
                        </span>
                      </Link>
                    </td>
                    <td className="whitespace-nowrap px-5 py-3.5 text-xs text-slate-400">
                      {formatDate(scan.createdAt)}
                    </td>
                    <td className="px-5 py-3.5">
                      <span
                        className={`rounded-md px-2 py-0.5 text-[11px] font-semibold uppercase ring-1 ${
                          STATUS_STYLES[scan.status] ??
                          'bg-slate-500/15 text-slate-400 ring-slate-500/30'
                        }`}
                      >
                        {scan.status}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="flex items-baseline gap-1.5">
                        <span
                          className={`text-base font-semibold ${
                            scan.overallScore === null ? 'text-slate-500' : 'text-white'
                          }`}
                        >
                          {scan.overallScore === null ? '—' : scan.overallScore}
                        </span>
                        {scan.overallScore !== null && (
                          <span className="text-[11px] text-slate-500">{band.label}</span>
                        )}
                        {scan.overallScore === null && (
                          <span className="text-[11px] text-slate-500">not enough data</span>
                        )}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-slate-400">
                      {scan.status === 'COMPLETED' ? (scan.checksFailed ?? 0) : '—'}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Link
                          to={`/report/${scan.id}`}
                          className="rounded-lg border border-white/10 px-2.5 py-1 text-xs text-slate-300 transition hover:border-cyanx-400/40 hover:text-cyanx-300"
                        >
                          Open
                        </Link>
                        <button
                          type="button"
                          aria-label={`Delete report ${scan.id}`}
                          title="Delete this report and its conversation"
                          onClick={() => setPendingDelete(scan)}
                          className="rounded-lg border border-white/10 p-1.5 text-slate-500 transition hover:border-rose-400/40 hover:text-rose-300"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={pendingDelete !== null}
        title="Delete this report?"
        onClose={() => setPendingDelete(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setPendingDelete(null)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={() => void confirmDelete()}>
              <Trash2 className="h-4 w-4" /> Delete report
            </Button>
          </>
        }
      >
        Report #{pendingDelete?.id} for{' '}
        <span className="font-medium text-white">{pendingDelete ? hostOf(pendingDelete.url) : ''}</span>{' '}
        will be removed with its findings, scores and Webby conversation. This cannot be undone.
      </Modal>
    </div>
  );
}
