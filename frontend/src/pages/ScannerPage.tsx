import { ArrowRight, Clock, Globe, History } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import EmptyState from '../components/ui/EmptyState';
import Section from '../components/ui/Section';
import UrlScanForm from '../components/scanner/UrlScanForm';
import { api } from '../services/api';
import type { ScanSummary } from '../types/api';
import { formatDate, hostOf } from '../utils/format';

const CHECK_GROUPS = [
  { title: 'SEO', items: ['Title and description', 'Heading hierarchy', 'Canonical + robots', 'Image alt text (SEO)'] },
  { title: 'Accessibility basics', items: ['Image alt attributes', 'Form label associations', 'Accessible names', 'Document language'] },
  { title: 'Technical', items: ['HTTPS transport', 'Document structure', 'Viewport + charset', 'Link targets, mixed content'] },
  { title: 'Content structure', items: ['Meaningful text volume', 'Section headings', 'Navigation landmarks', 'Descriptive link text'] },
];

export default function ScannerPage() {
  const [recent, setRecent] = useState<ScanSummary[]>([]);
  const [recentError, setRecentError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .listScans()
      .then((scans) => {
        if (!cancelled) setRecent(scans.slice(0, 5));
      })
      .catch((e) => {
        if (!cancelled) setRecentError(e instanceof Error ? e.message : 'Could not load recent scans.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="space-y-10">
      <Section>
        <div className="max-w-2xl">
          <h1 className="text-3xl font-bold tracking-tight text-white">Website Scanner</h1>
          <p className="mt-2 text-sm leading-relaxed text-slate-400">
            The scanner downloads the public HTML of the page you choose and runs 25 documented
            checks across four categories. Everything in the report traces back to that document.
          </p>
        </div>

        <div className="card mt-6 max-w-2xl p-6">
          <UrlScanForm autoFocus />
        </div>
      </Section>

      <Section>
        <div className="grid gap-4 lg:grid-cols-2">
          <div className="card p-6">
            <h2 className="text-base font-semibold text-white">What gets checked</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              {CHECK_GROUPS.map((group) => (
                <div key={group.title}>
                  <p className="text-xs font-semibold uppercase tracking-wider text-cyanx-300">
                    {group.title}
                  </p>
                  <ul className="mt-2 space-y-1 text-sm text-slate-400">
                    {group.items.map((item) => (
                      <li key={item} className="flex gap-2">
                        <span className="text-slate-600">›</span>
                        {item}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
            <p className="mt-5 border-t border-white/[0.06] pt-4 text-xs text-slate-500">
              Not measured: page speed, third-party scripts, server configuration, security
              vulnerabilities and anything behind a login. Those need different tools.
            </p>
          </div>

          <div className="card p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-semibold text-white">Recent scans</h2>
              <Link to="/history" className="inline-flex items-center gap-1 text-xs text-slate-400 hover:text-cyanx-300">
                Full history <ArrowRight className="h-3 w-3" />
              </Link>
            </div>

            {recentError && <p className="mt-4 text-sm text-rose-300">{recentError}</p>}

            {!recentError && recent.length === 0 && (
              <div className="mt-4">
                <EmptyState
                  icon={<History className="h-6 w-6" />}
                  title="No scans yet"
                  description="Run your first scan and it will appear here, together with its score and findings."
                />
              </div>
            )}

            {recent.length > 0 && (
              <ul className="mt-4 divide-y divide-white/[0.05]">
                {recent.map((scan) => (
                  <li key={scan.id}>
                    <Link
                      to={`/report/${scan.id}`}
                      className="flex items-center gap-3 py-3 transition hover:opacity-80"
                    >
                      <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/[0.05] text-slate-400">
                        <Globe className="h-4 w-4" />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-sm text-slate-200">{hostOf(scan.url)}</span>
                        <span className="flex items-center gap-1.5 text-xs text-slate-500">
                          <Clock className="h-3 w-3" /> {formatDate(scan.createdAt)}
                        </span>
                      </span>
                      <span
                        className={`text-sm font-semibold ${
                          scan.overallScore === null ? 'text-slate-500' : 'text-cyanx-300'
                        }`}
                      >
                        {scan.overallScore === null ? '—' : scan.overallScore}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </Section>
    </div>
  );
}
