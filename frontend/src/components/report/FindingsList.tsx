import { AnimatePresence, motion } from 'framer-motion';
import { Check, ChevronDown, Code2, Copy, Sparkles } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useApp } from '../../context/AppContext';
import type { Finding } from '../../types/api';
import { severityClass, severityRank, STATUS_LABEL } from '../../utils/severity';
import EmptyState from '../ui/EmptyState';

type Filter = 'ISSUES' | 'PASSED' | 'ALL';

interface Props {
  findings: Finding[];
}

export default function FindingsList({ findings }: Props) {
  const [filter, setFilter] = useState<Filter>('ISSUES');
  const [expanded, setExpanded] = useState<Set<number>>(new Set());
  const [copiedId, setCopiedId] = useState<number | null>(null);
  const { openCopilot, pushToast } = useApp();

  const visible = useMemo(() => {
    const filtered = findings.filter((finding) => {
      if (filter === 'ISSUES') return finding.status === 'FAIL';
      if (filter === 'PASSED') return finding.status === 'PASS';
      return finding.status !== 'NOT_APPLICABLE';
    });
    return [...filtered].sort((a, b) => {
      if (a.status !== b.status) return a.status === 'FAIL' ? -1 : 1;
      const severityDiff = severityRank(b.severity) - severityRank(a.severity);
      if (severityDiff !== 0) return severityDiff;
      return a.category.localeCompare(b.category);
    });
  }, [findings, filter]);

  const counts = {
    ISSUES: findings.filter((f) => f.status === 'FAIL').length,
    PASSED: findings.filter((f) => f.status === 'PASS').length,
    ALL: findings.filter((f) => f.status !== 'NOT_APPLICABLE').length,
  };

  const toggle = (id: number) => {
    setExpanded((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const copyCode = async (finding: Finding) => {
    if (!finding.codeExample) return;
    try {
      await navigator.clipboard.writeText(finding.codeExample);
      setCopiedId(finding.id);
      pushToast('Code example copied to clipboard.', 'success');
      window.setTimeout(() => setCopiedId(null), 1800);
    } catch {
      pushToast('Clipboard access was blocked by the browser.', 'error');
    }
  };

  return (
    <div className="card overflow-hidden">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/[0.07] px-5 py-4">
        <h2 className="text-base font-semibold text-white">Findings</h2>
        <div className="flex gap-1 rounded-xl border border-white/[0.07] bg-white/[0.03] p-1">
          {(['ISSUES', 'PASSED', 'ALL'] as Filter[]).map((value) => (
            <button
              key={value}
              type="button"
              onClick={() => setFilter(value)}
              className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                filter === value ? 'bg-white/[0.09] text-white' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {value === 'ISSUES' ? 'Issues' : value === 'PASSED' ? 'Passed' : 'All checks'}
              <span className="ml-1.5 text-[11px] text-slate-500">{counts[value]}</span>
            </button>
          ))}
        </div>
      </div>

      {visible.length === 0 ? (
        <div className="p-5">
          <EmptyState
            icon={<Check className="h-6 w-6" />}
            title={filter === 'ISSUES' ? 'No issues in this view' : 'Nothing here yet'}
            description={
              filter === 'ISSUES'
                ? 'Every check in this filter passed - switch to "All checks" to see the evidence behind each result.'
                : 'No checks matched this filter.'
            }
          />
        </div>
      ) : (
        <ul className="divide-y divide-white/[0.05]">
          {visible.map((finding) => {
            const isOpen = expanded.has(finding.id);
            const isIssue = finding.status === 'FAIL';
            return (
              <li key={finding.id} id={`finding-${finding.id}`} className="scroll-mt-24">
                <button
                  type="button"
                  onClick={() => toggle(finding.id)}
                  aria-expanded={isOpen}
                  className="flex w-full items-start gap-3 px-5 py-4 text-left transition hover:bg-white/[0.02]"
                >
                  <span
                    className={`mt-0.5 shrink-0 rounded-md px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ring-1 ${
                      isIssue
                        ? severityClass(finding.severity)
                        : finding.status === 'PASS'
                          ? 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30'
                          : 'bg-slate-500/15 text-slate-400 ring-slate-500/30'
                    }`}
                  >
                    {isIssue ? finding.severity : STATUS_LABEL[finding.status]}
                  </span>

                  <span className="min-w-0 flex-1">
                    <span className="flex flex-wrap items-center gap-2">
                      <span className="text-sm font-medium text-slate-100">{finding.title}</span>
                      <span className="rounded-md bg-white/[0.05] px-1.5 py-0.5 text-[10px] uppercase tracking-wide text-slate-500">
                        {finding.category}
                      </span>
                    </span>
                    <span className="mt-1 block truncate text-xs text-slate-500">{finding.evidence}</span>
                  </span>

                  <ChevronDown
                    className={`mt-1 h-4 w-4 shrink-0 text-slate-500 transition-transform ${
                      isOpen ? 'rotate-180' : ''
                    }`}
                  />
                </button>

                <AnimatePresence initial={false}>
                  {isOpen && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.22, ease: 'easeOut' }}
                      className="overflow-hidden"
                    >
                      <div className="space-y-4 bg-white/[0.015] px-5 py-4">
                        <Block label="Evidence">
                          <p className="text-sm leading-relaxed text-slate-300">{finding.evidence}</p>
                        </Block>

                        {finding.explanation && (
                          <Block label="Why it matters">
                            <p className="text-sm leading-relaxed text-slate-400">{finding.explanation}</p>
                          </Block>
                        )}

                        {isIssue && finding.recommendation && (
                          <Block label="Recommended fix">
                            <p className="text-sm leading-relaxed text-slate-300">{finding.recommendation}</p>
                          </Block>
                        )}

                        {finding.codeExample && (
                          <Block label="Example code">
                            <div className="relative">
                              <pre className="thin-scroll overflow-x-auto rounded-xl border border-white/[0.07] bg-navy-950/80 p-4 text-xs leading-relaxed text-cyanx-100">
                                <code>{finding.codeExample}</code>
                              </pre>
                              <button
                                type="button"
                                onClick={() => void copyCode(finding)}
                                className="absolute right-2 top-2 inline-flex items-center gap-1.5 rounded-lg border border-white/10 bg-navy-900/90 px-2.5 py-1.5 text-[11px] text-slate-300 transition hover:border-cyanx-400/40 hover:text-cyanx-300"
                              >
                                {copiedId === finding.id ? (
                                  <><Check className="h-3 w-3" /> Copied</>
                                ) : (
                                  <><Copy className="h-3 w-3" /> Copy</>
                                )}
                              </button>
                            </div>
                            <p className="mt-2 text-[11px] text-slate-500">
                              Illustrative snippet - WebScorer never changes your site.
                            </p>
                          </Block>
                        )}

                        <div className="flex flex-wrap items-center gap-2 pt-1">
                          <button
                            type="button"
                            onClick={() =>
                              openCopilot(`How can I fix this issue? "${finding.title}" - ${finding.evidence}`)
                            }
                            className="inline-flex items-center gap-1.5 rounded-lg border border-cyanx-400/30 bg-cyanx-500/10 px-3 py-1.5 text-xs font-medium text-cyanx-200 transition hover:bg-cyanx-500/20"
                          >
                            <Sparkles className="h-3.5 w-3.5" /> Ask Webby
                          </button>
                          {finding.codeExample && (
                            <button
                              type="button"
                              onClick={() => void copyCode(finding)}
                              className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-1.5 text-xs text-slate-300 transition hover:bg-white/[0.06]"
                            >
                              <Code2 className="h-3.5 w-3.5" /> Copy code
                            </button>
                          )}
                          <span className="text-[11px] text-slate-600">check id: {finding.checkId}</span>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

function Block({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p className="mb-1.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">{label}</p>
      {children}
    </div>
  );
}
