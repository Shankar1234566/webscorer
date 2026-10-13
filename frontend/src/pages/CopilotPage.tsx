import { Bot, MessageSquare, Sparkles, ThumbsUp } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import EmptyState from '../components/ui/EmptyState';
import Section from '../components/ui/Section';
import Button from '../components/ui/Button';
import WebbyRobot from '../components/webby/WebbyRobot';
import { useApp } from '../context/AppContext';
import { api } from '../services/api';
import type { CopilotMessage, HealthResponse } from '../types/api';
import { formatDate, hostOf } from '../utils/format';

const QUESTIONS = [
  { q: 'What should I fix first?', hint: 'Ranked by severity and score weight' },
  { q: 'Explain my website score', hint: 'How each category number was produced' },
  { q: 'Explain the SEO findings', hint: 'Title, description, headings, canonical' },
  { q: 'Generate an example code fix', hint: 'Copy-ready snippet for a real finding' },
  { q: 'What does this finding mean for users?', hint: 'Plain-language impact' },
  { q: 'Why does this matter for accessibility?', hint: 'Assistive-technology perspective' },
];

/**
 * Launchpad for the copilot: shows which report Webby is reading, how the
 * answers are produced (AI key or local guidance) and the stored conversation.
 * The chat itself lives in the floating panel so it is available on every page.
 */
export default function CopilotPage() {
  const { scanDetail, openCopilot, copilotOpen, setCopilotOpen } = useApp();
  const [history, setHistory] = useState<CopilotMessage[]>([]);
  const [health, setHealth] = useState<HealthResponse | null>(null);

  const scanId = scanDetail?.scan.id ?? null;

  useEffect(() => {
    api.health().then(setHealth).catch(() => setHealth(null));
  }, []);

  useEffect(() => {
    if (scanId === null) {
      setHistory([]);
      return;
    }
    let cancelled = false;
    api
      .copilotHistory(scanId)
      .then((messages) => {
        if (!cancelled) setHistory(messages);
      })
      .catch(() => {
        if (!cancelled) setHistory([]);
      });
    return () => {
      cancelled = true;
    };
  }, [scanId, copilotOpen]);

  if (scanId === null) {
    return (
      <EmptyState
        icon={<Bot className="h-6 w-6" />}
        title="Webby needs a report to read"
        description="The copilot only answers from a finished scan - it will not make things up. Run a scan first, then come back here."
        action={
          <Link
            to="/scanner"
            className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-electric-500 to-violetish-600 px-4 py-2.5 text-sm font-medium text-white"
          >
            Open the scanner
          </Link>
        }
      />
    );
  }

  return (
    <div className="space-y-6">
      <Section>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-white">Copilot</h1>
            <p className="mt-2 max-w-2xl text-sm leading-relaxed text-slate-400">
              Webby reads the report for{' '}
              <span className="text-slate-200">{hostOf(scanDetail!.scan.url)}</span> - scores,
              evidence and every finding - and answers from that context only.
            </p>
          </div>
          <Button onClick={() => (copilotOpen ? setCopilotOpen(false) : openCopilot(null))}>
            <MessageSquare className="h-4 w-4" />
            {copilotOpen ? 'Hide chat panel' : 'Open chat panel'}
          </Button>
        </div>

        <div className="mt-5 grid gap-4 lg:grid-cols-[1fr_20rem]">
          <div className="card p-5">
            <div className="flex items-center gap-3">
              <span className="flex h-11 w-11 items-center justify-center rounded-full bg-navy-800">
                <WebbyRobot mood="happy" size={38} isStatic />
              </span>
              <div>
                <p className="text-sm font-semibold text-white">Webby</p>
                <p className="text-xs text-slate-500">
                  Report #{scanId} · score{' '}
                  {scanDetail!.scan.overallScore === null
                    ? 'not available'
                    : `${scanDetail!.scan.overallScore}/100`}
                </p>
              </div>
            </div>

            <p className="mt-4 text-sm leading-relaxed text-slate-400">
              Ask about priorities, evidence, code fixes or what a finding means for real visitors.
              Webby will say so when the report does not contain an answer.
            </p>

            <div className="mt-4 grid gap-2 sm:grid-cols-2">
              {QUESTIONS.map((item) => (
                <button
                  key={item.q}
                  type="button"
                  onClick={() => openCopilot(item.q)}
                  className="rounded-xl border border-white/[0.07] bg-white/[0.03] p-3 text-left transition hover:border-cyanx-400/30 hover:bg-white/[0.05]"
                >
                  <span className="flex items-center gap-1.5 text-sm text-slate-200">
                    <Sparkles className="h-3.5 w-3.5 text-cyanx-300" /> {item.q}
                  </span>
                  <span className="mt-1 block text-xs text-slate-500">{item.hint}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="card p-5">
            <h2 className="text-sm font-semibold text-white">Answer source</h2>
            <div className="mt-3 space-y-2 text-sm">
              <p className="flex items-center justify-between gap-2">
                <span className="text-slate-500">Mode</span>
                <span
                  className={`rounded-md px-2 py-0.5 text-xs font-medium ring-1 ${
                    health?.copilotMode === 'llm'
                      ? 'bg-violetish-500/15 text-violetish-300 ring-violetish-400/30'
                      : 'bg-cyanx-500/15 text-cyanx-300 ring-cyanx-400/30'
                  }`}
                >
                  {health === null
                    ? 'Unknown'
                    : health.copilotMode === 'llm'
                      ? 'AI provider'
                      : 'Local guidance'}
                </span>
              </p>
              <p className="flex items-center justify-between gap-2">
                <span className="text-slate-500">Backend</span>
                <span className="text-slate-300">{health?.status ?? '—'}</span>
              </p>
            </div>
            <p className="mt-4 border-t border-white/[0.06] pt-3 text-xs leading-relaxed text-slate-500">
              {health?.copilotMode === 'llm'
                ? 'An API key is configured on the server, so replies come from the AI provider. Only report metadata is sent - never the page content.'
                : 'No AI key is configured, so Webby answers from the saved report using built-in rules. Replies are labelled "Local guidance".'}
            </p>

            <div className="mt-4 border-t border-white/[0.06] pt-3">
              <p className="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <ThumbsUp className="h-3 w-3" /> Stored conversation
              </p>
              {history.length === 0 ? (
                <p className="text-xs text-slate-500">
                  No messages yet - your chat with Webby will be saved here.
                </p>
              ) : (
                <ul className="thin-scroll max-h-52 space-y-2 overflow-y-auto pr-1">
                  {history.slice(-8).map((message) => (
                    <li
                      key={message.id}
                      className={`rounded-lg px-2.5 py-2 text-xs leading-relaxed ${
                        message.role === 'USER'
                          ? 'bg-electric-500/15 text-slate-200'
                          : 'bg-white/[0.04] text-slate-400'
                      }`}
                    >
                      <span className="mb-0.5 block text-[10px] uppercase tracking-wide text-slate-500">
                        {message.role === 'USER' ? 'You' : 'Webby'} · {formatDate(message.createdAt)}
                      </span>
                      {message.message.length > 160
                        ? `${message.message.slice(0, 160)}…`
                        : message.message}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </div>
      </Section>
    </div>
  );
}
