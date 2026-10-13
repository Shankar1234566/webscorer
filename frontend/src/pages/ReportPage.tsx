import { motion } from 'framer-motion';
import {
  AlertTriangle,
  Download,
  ExternalLink,
  History,
  RefreshCw,
  Sparkles,
  Timer,
} from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import CategoryCards from '../components/report/CategoryCards';
import FindingsList from '../components/report/FindingsList';
import ScanProgress from '../components/report/ScanProgress';
import ScoreCharts from '../components/report/ScoreCharts';
import ReportSkeleton from '../components/ui/Skeleton';
import Button from '../components/ui/Button';
import ScoreRing from '../components/ui/ScoreRing';
import { useApp } from '../context/AppContext';
import { useScanPolling } from '../hooks/useScanPolling';
import { api, ApiRequestError } from '../services/api';
import { sampleReport } from '../services/sampleReport';
import type { ScanDetail } from '../types/api';
import { deltaLabel, formatDate, hostOf, scoreBand } from '../utils/format';

export default function ReportPage() {
  const { scanId } = useParams<{ scanId: string }>();
  const navigate = useNavigate();
  const { setScanDetail, setActiveScan, pushToast } = useApp();

  const isSample = scanId === 'sample';
  const isLatest = scanId === 'latest';
  const [latestId, setLatestId] = useState<string | null>(null);
  const [latestError, setLatestError] = useState<string | null>(null);

  useEffect(() => {
    if (!isLatest) return;
    let cancelled = false;
    api
      .listScans()
      .then((scans) => {
        if (cancelled) return;
        if (scans.length === 0) {
          navigate('/history', { replace: true });
          return;
        }
        setLatestId(String(scans[0].id));
      })
      .catch((e) => {
        if (!cancelled) setLatestError(e instanceof ApiRequestError ? e.message : 'Could not load the latest report.');
      });
    return () => {
      cancelled = true;
    };
  }, [isLatest, navigate]);

  const polledId = isSample || isLatest ? latestId : (scanId ?? null);
  const { summary, detail, loading, error } = useScanPolling(polledId);

  // Publish state so Webby and the copilot panel know what is on screen.
  useEffect(() => {
    if (summary) setActiveScan(summary);
  }, [summary, setActiveScan]);

  useEffect(() => {
    if (detail) setScanDetail(detail);
  }, [detail, setScanDetail]);

  if (isSample) {
    return <ReportView detail={sampleReport} onRescan={() => navigate('/scanner')} />;
  }

  if (latestError) {
    return <ProblemCard title="Could not load the latest report" message={latestError} />;
  }

  if (error) {
    return <ProblemCard title="Could not load this report" message={error} />;
  }

  if (isLatest && latestId === null) {
    return <ReportSkeleton />;
  }

  if (summary && (summary.status === 'VALIDATING' || summary.status === 'FETCHING'
      || summary.status === 'ANALYZING' || summary.status === 'SCORING' || summary.status === 'PENDING')) {
    return <ScanProgress status={summary.status} stage={summary.stage} url={summary.url} />;
  }

  if (summary && summary.status === 'FAILED') {
    return (
      <ProblemCard
        title="The scan stopped before a report could be built"
        message={summary.errorMessage ?? 'Unknown error.'}
        action={
          <div className="flex flex-wrap gap-3">
            <Button onClick={() => navigate('/scanner')}>Try another URL</Button>
            <Button variant="secondary" onClick={() => window.location.reload()}>
              Run it again
            </Button>
          </div>
        }
      />
    );
  }

  if (detail) {
    return <ReportView detail={detail} onRescan={() => void rescan(detail)} />;
  }

  if (loading || !summary) {
    return <ReportSkeleton />;
  }

  return <ReportSkeleton />;

  async function rescan(target: ScanDetail) {
    try {
      const scan = await api.createScan(target.scan.url);
      setActiveScan(scan);
      pushToast('Re-scanning the same URL.', 'info');
      navigate(`/report/${scan.id}`);
    } catch (e) {
      pushToast(e instanceof ApiRequestError ? e.message : 'Could not start the rescan.', 'error');
    }
  }
}

function ProblemCard({
  title,
  message,
  action,
}: {
  title: string;
  message: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="card mx-auto max-w-2xl p-8 text-center">
      <span className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-500/15 text-rose-300">
        <AlertTriangle className="h-6 w-6" />
      </span>
      <h2 className="mt-4 text-lg font-semibold text-white">{title}</h2>
      <p className="mx-auto mt-2 max-w-lg text-sm leading-relaxed text-slate-400">{message}</p>
      {action && <div className="mt-5 flex justify-center">{action}</div>}
      <Link to="/scanner" className="mt-4 inline-block text-sm text-cyanx-300 hover:underline">
        Back to the scanner
      </Link>
    </div>
  );
}

function ReportView({ detail, onRescan }: { detail: ScanDetail; onRescan: () => void }) {
  const { scan } = detail;
  const { openCopilot } = useApp();
  const band = scoreBand(scan.overallScore);
  const issues = detail.findings.filter((finding) => finding.status === 'FAIL');
  const delta = deltaLabel(scan.overallScore, detail.previousScore);

  return (
    <div className="space-y-6">
      {detail.sample && (
        <div className="rounded-xl border border-amber-400/30 bg-amber-500/10 px-4 py-3 text-sm text-amber-200">
          <strong className="font-semibold">Sample report.</strong> This is hand-written demonstration
          data so you can see the layout. Run a real scan to get live findings and to chat with Webby.
        </div>
      )}

      {/* ---------------------------------------------------------- header */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="card p-6"
      >
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-md bg-emerald-500/15 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide text-emerald-300 ring-1 ring-emerald-500/30">
                {scan.status === 'COMPLETED' ? 'Completed' : scan.status}
              </span>
              <span className="text-xs text-slate-500">Report #{scan.id}</span>
            </div>

            <h1 className="mt-2 truncate text-2xl font-bold text-white" title={scan.url}>
              {hostOf(scan.url)}
            </h1>
            <p className="mt-1 truncate text-sm text-slate-400" title={scan.finalUrl ?? scan.url}>
              {scan.pageTitle ?? scan.finalUrl ?? scan.url}
            </p>

            <div className="mt-4 flex flex-wrap gap-x-5 gap-y-2 text-xs text-slate-500">
              <span className="inline-flex items-center gap-1.5">
                <History className="h-3.5 w-3.5" /> {formatDate(scan.completedAt ?? scan.createdAt)}
              </span>
              {scan.httpStatus !== null && (
                <span className="inline-flex items-center gap-1.5">
                  <ExternalLink className="h-3.5 w-3.5" /> HTTP {scan.httpStatus}
                </span>
              )}
              {scan.durationMs !== null && (
                <span className="inline-flex items-center gap-1.5">
                  <Timer className="h-3.5 w-3.5" /> {scan.durationMs} ms
                </span>
              )}
              <span>
                {scan.checksPassed ?? 0} passed · {scan.checksFailed ?? 0} failed
                {(scan.checksNotRun ?? 0) > 0 ? ` · ${scan.checksNotRun} not run` : ''}
              </span>
            </div>

            <div className="mt-4 flex flex-wrap items-center gap-3">
              <Button
                onClick={() => openCopilot('What should I fix first?')}
                disabled={detail.sample}
                title={detail.sample ? 'Available once you run a real scan' : undefined}
              >
                <Sparkles className="h-4 w-4" /> Ask Webby
              </Button>
              <a
                href={api.exportUrl(scan.id)}
                download
                className="inline-flex items-center gap-2 rounded-xl border border-white/12 bg-white/[0.05] px-4 py-2.5 text-sm text-slate-100 transition hover:border-cyanx-400/40 hover:bg-white/[0.08]"
              >
                <Download className="h-4 w-4" /> Export CSV
              </a>
              <Button variant="secondary" onClick={onRescan} disabled={detail.sample}>
                <RefreshCw className="h-4 w-4" /> Scan again
              </Button>
              <Link
                to="/history"
                className="text-sm text-slate-400 underline decoration-white/20 underline-offset-4 hover:text-cyanx-300"
              >
                History
              </Link>
            </div>
          </div>

          <div className="flex flex-col items-center gap-3 lg:border-l lg:border-white/[0.06] lg:pl-8">
            <ScoreRing score={scan.overallScore} />
            <span
              className={`rounded-full px-3 py-1 text-xs font-medium ring-1 ${
                band.tone === 'good'
                  ? 'bg-cyanx-500/10 text-cyanx-300 ring-cyanx-400/30'
                  : band.tone === 'ok'
                    ? 'bg-slate-500/10 text-slate-300 ring-slate-400/30'
                    : 'bg-rose-500/10 text-rose-300 ring-rose-400/30'
              }`}
            >
              {band.label}
            </span>
            {delta && <p className="text-xs text-slate-500">{delta}</p>}
            {detail.previousScore !== null && detail.previousScanAt && (
              <Link
                to={`/report/${detail.previousScanId}`}
                className="text-[11px] text-slate-500 underline decoration-white/20 underline-offset-2 hover:text-cyanx-300"
              >
                Previous scan: {detail.previousScore}/100 · {formatDate(detail.previousScanAt)}
              </Link>
            )}
          </div>
        </div>

        {scan.summary && (
          <p className="mt-5 border-t border-white/[0.06] pt-4 text-sm leading-relaxed text-slate-400">
            {scan.summary}
          </p>
        )}
      </motion.div>

      <CategoryCards scores={detail.categoryScores} />
      <ScoreCharts categoryScores={detail.categoryScores} findings={detail.findings} />

      {/* ---------------------------------------------------- priorities */}
      {issues.length > 0 && (
        <div className="card p-5">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-base font-semibold text-white">Improvement priorities</h2>
            <span className="text-xs text-slate-500">Sorted by severity, then score weight</span>
          </div>
          <ol className="mt-4 space-y-3">
            {issues.slice(0, 3).map((issue, index) => (
              <li key={issue.id} className="flex items-start gap-3">
                <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-electric-500 to-violetish-600 text-[11px] font-bold text-white">
                  {index + 1}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-slate-100">{issue.title}</p>
                  <p className="mt-0.5 truncate text-xs text-slate-500">{issue.evidence}</p>
                </div>
                <a
                  href={`#finding-${issue.id}`}
                  className="shrink-0 rounded-lg border border-white/10 px-2.5 py-1 text-xs text-slate-300 transition hover:border-cyanx-400/40 hover:text-cyanx-300"
                >
                  View
                </a>
              </li>
            ))}
          </ol>
        </div>
      )}

      <FindingsList findings={detail.findings} />

      {/* -------------------------------------------------- scoring notes */}
      <details className="card p-5">
        <summary className="cursor-pointer text-sm font-semibold text-white">
          How was this score calculated?
        </summary>
        <ul className="mt-3 space-y-1.5 text-sm text-slate-400">
          {detail.scoringNotes.map((note) => (
            <li key={note} className="flex gap-2">
              <span className="text-slate-600">•</span>
              {note}
            </li>
          ))}
        </ul>
        <p className="mt-3 text-xs leading-relaxed text-slate-500">
          Only checks that actually ran enter the denominator: a check that could not be executed is
          recorded as “not run” and never lowers a score. Scoring rules are documented in
          docs/scoring-rules.md.
        </p>
      </details>
    </div>
  );
}
