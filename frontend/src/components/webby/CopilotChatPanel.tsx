import { AnimatePresence, motion } from 'framer-motion';
import { AlertTriangle, ArrowUp, Check, Loader2, MessageSquare, Trash2, X } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import { api, ApiRequestError } from '../../services/api';
import Modal from '../ui/Modal';
import WebbyRobot from './WebbyRobot';

interface ChatMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  text: string;
  provider?: string | null;
  note?: string | null;
}

const DEFAULT_SUGGESTIONS = [
  'What should I fix first?',
  'Explain my website score',
  'Explain the SEO findings',
  'Generate an example code fix',
  'What does this finding mean for users?',
];

/**
 * Webby's chat panel. Always scoped to the report currently open, so every
 * answer is grounded in real scan data.
 */
export default function CopilotChatPanel() {
  const { copilotOpen, setCopilotOpen, scanDetail, copilotPrefill, pushToast } = useApp();
  const scanId = scanDetail?.scan.id ?? null;

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmClear, setConfirmClear] = useState(false);
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const lastUserQuestion = useRef<string | null>(null);

  // The backend returns its own suggestion list per answer; until the first
  // reply arrives we offer the same set the copilot documents.
  const suggestions = DEFAULT_SUGGESTIONS;

  // Load persisted chat history when the panel opens for a report.
  useEffect(() => {
    if (!copilotOpen || scanId === null) return;
    let cancelled = false;
    setLoadingHistory(true);
    api
      .copilotHistory(scanId)
      .then((history) => {
        if (cancelled) return;
        setMessages(
          history.map((item) => ({
            id: String(item.id),
            role: item.role,
            text: item.message,
            provider: item.provider,
          })),
        );
      })
      .catch(() => {
        // History is a nice-to-have; the panel still works for new messages.
      })
      .finally(() => {
        if (!cancelled) setLoadingHistory(false);
      });
    return () => {
      cancelled = true;
    };
  }, [copilotOpen, scanId]);

  // Prefill from "Ask Webby" buttons elsewhere in the app.
  useEffect(() => {
    if (copilotOpen && copilotPrefill) {
      setInput(copilotPrefill);
    }
  }, [copilotOpen, copilotPrefill]);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, sending]);

  const send = useCallback(
    async (question: string) => {
      const trimmed = question.trim();
      if (!trimmed || scanId === null || sending) return;
      lastUserQuestion.current = trimmed;
      setError(null);
      setInput('');
      setSending(true);
      setMessages((current) => [
        ...current,
        { id: `local-${Date.now()}`, role: 'USER', text: trimmed },
      ]);
      try {
        const response = await api.askCopilot(scanId, trimmed);
        setMessages((current) => [
          ...current,
          {
            id: `bot-${Date.now()}`,
            role: 'ASSISTANT',
            text: response.message,
            provider: response.provider,
            note: response.note,
          },
        ]);
      } catch (e) {
        const message =
          e instanceof ApiRequestError ? e.message : 'Webby could not answer that just now.';
        setError(message);
      } finally {
        setSending(false);
      }
    },
    [scanId, sending],
  );

  const clearChat = async () => {
    if (scanId === null) return;
    try {
      await api.clearCopilot(scanId);
      setMessages([]);
      pushToast('Conversation cleared.', 'success');
    } catch (e) {
      pushToast(e instanceof ApiRequestError ? e.message : 'Could not clear the conversation.', 'error');
    } finally {
      setConfirmClear(false);
    }
  };

  return (
    <>
      <AnimatePresence>
        {copilotOpen && (
          <motion.aside
            key="copilot-panel"
            initial={{ opacity: 0, y: 24, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 16, scale: 0.98 }}
            transition={{ duration: 0.22, ease: 'easeOut' }}
            aria-label="Webby copilot chat"
            className="fixed bottom-4 right-4 z-50 flex h-[min(34rem,75vh)] w-[min(23rem,calc(100vw-2rem))] flex-col overflow-hidden rounded-2xl border border-white/10 bg-navy-900/95 shadow-card backdrop-blur-xl sm:bottom-6 sm:right-6"
          >
            <header className="flex items-center gap-3 border-b border-white/[0.07] bg-white/[0.03] px-4 py-3">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-navy-800">
                <WebbyRobot mood="happy" size={30} isStatic />
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-white">Webby</p>
                <p className="truncate text-[11px] text-slate-400">
                  {scanId === null
                    ? 'No report selected'
                    : `Copilot for report #${scanId}`}
                </p>
              </div>
              <button
                type="button"
                aria-label="Clear conversation"
                title="Clear conversation"
                onClick={() => setConfirmClear(true)}
                className="rounded-lg p-1.5 text-slate-400 transition hover:bg-white/10 hover:text-white"
              >
                <Trash2 className="h-4 w-4" />
              </button>
              <button
                type="button"
                aria-label="Close copilot"
                onClick={() => setCopilotOpen(false)}
                className="rounded-lg p-1.5 text-slate-400 transition hover:bg-white/10 hover:text-white"
              >
                <X className="h-4 w-4" />
              </button>
            </header>

            {scanId === null ? (
              <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
                <MessageSquare className="h-7 w-7 text-cyanx-300" />
                <p className="text-sm text-slate-300">
                  Webby answers from a finished report. Run a scan first, then ask anything about it.
                </p>
                <Link
                  to="/scanner"
                  onClick={() => setCopilotOpen(false)}
                  className="rounded-xl bg-gradient-to-r from-electric-500 to-violetish-600 px-4 py-2 text-sm font-medium text-white"
                >
                  Open the scanner
                </Link>
              </div>
            ) : (
              <>
                <div ref={scrollRef} className="thin-scroll flex-1 space-y-3 overflow-y-auto px-4 py-4">
                  {loadingHistory && (
                    <p className="text-center text-xs text-slate-500">Loading earlier messages…</p>
                  )}
                  {!loadingHistory && messages.length === 0 && (
                    <div className="rounded-xl border border-white/[0.07] bg-white/[0.03] p-3 text-xs leading-relaxed text-slate-400">
                      Hi, I am Webby. I have read this report - scores, evidence and every finding.
                      Ask what to fix first, or pick a suggestion below.
                    </div>
                  )}
                  {messages.map((message) => (
                    <div
                      key={message.id}
                      className={`flex ${message.role === 'USER' ? 'justify-end' : 'justify-start'}`}
                    >
                      <div
                        className={`max-w-[85%] rounded-2xl px-3.5 py-2.5 text-[13px] leading-relaxed ${
                          message.role === 'USER'
                            ? 'bg-electric-500/25 text-slate-100 ring-1 ring-electric-400/30'
                            : 'border border-white/[0.07] bg-white/[0.04] text-slate-200'
                        }`}
                      >
                        {message.role === 'ASSISTANT' && (
                          <span
                            className={`mb-1.5 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide ${
                              message.provider === 'LLM'
                                ? 'bg-violetish-500/20 text-violetish-400'
                                : 'bg-cyanx-500/15 text-cyanx-300'
                            }`}
                          >
                            {message.provider === 'LLM' ? 'AI provider' : 'Local guidance'}
                          </span>
                        )}
                        <p className="whitespace-pre-wrap">{message.text}</p>
                        {message.note && (
                          <p className="mt-2 border-t border-white/[0.07] pt-2 text-[11px] italic text-slate-500">
                            {message.note}
                          </p>
                        )}
                      </div>
                    </div>
                  ))}

                  {sending && (
                    <div className="flex justify-start">
                      <div className="flex items-center gap-2 rounded-2xl border border-white/[0.07] bg-white/[0.04] px-3.5 py-2.5">
                        <Loader2 className="h-3.5 w-3.5 animate-spin text-cyanx-300" />
                        <span className="text-xs text-slate-400">Webby is thinking…</span>
                      </div>
                    </div>
                  )}

                  {error && (
                    <div className="rounded-xl border border-rose-400/30 bg-rose-500/10 p-3 text-xs text-rose-200">
                      <div className="flex items-start gap-2">
                        <AlertTriangle className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                        <div>
                          <p>{error}</p>
                          {lastUserQuestion.current && (
                            <button
                              type="button"
                              onClick={() => void send(lastUserQuestion.current ?? '')}
                              className="mt-2 rounded-lg border border-white/15 px-2.5 py-1 text-[11px] text-white transition hover:bg-white/10"
                            >
                              Try again
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                <div className="border-t border-white/[0.07] px-4 pb-3 pt-2.5">
                  <div className="thin-scroll mb-2 flex gap-1.5 overflow-x-auto pb-1">
                    {suggestions.map((suggestion) => (
                      <button
                        key={suggestion}
                        type="button"
                        onClick={() => void send(suggestion)}
                        disabled={sending}
                        className="chip shrink-0 whitespace-nowrap"
                      >
                        {suggestion}
                      </button>
                    ))}
                  </div>
                  <form
                    onSubmit={(event) => {
                      event.preventDefault();
                      void send(input);
                    }}
                    className="flex items-end gap-2"
                  >
                    <textarea
                      value={input}
                      onChange={(event) => setInput(event.target.value)}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter' && !event.shiftKey) {
                          event.preventDefault();
                          void send(input);
                        }
                      }}
                      rows={1}
                      maxLength={2000}
                      placeholder="Ask Webby about this report…"
                      aria-label="Message Webby"
                      className="input-field max-h-28 min-h-[44px] resize-none py-2.5 text-[13px]"
                    />
                    <button
                      type="submit"
                      aria-label="Send message"
                      disabled={sending || input.trim().length === 0}
                      className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-electric-500 to-violetish-600 text-white transition disabled:opacity-40"
                    >
                      {sending ? <Loader2 className="h-4 w-4 animate-spin" /> : <ArrowUp className="h-4 w-4" />}
                    </button>
                  </form>
                </div>
              </>
            )}
          </motion.aside>
        )}
      </AnimatePresence>

      <Modal
        open={confirmClear}
        title="Clear this conversation?"
        onClose={() => setConfirmClear(false)}
        footer={
          <>
            <button
              type="button"
              onClick={() => setConfirmClear(false)}
              className="rounded-xl border border-white/12 px-4 py-2 text-sm text-slate-300 transition hover:bg-white/[0.06]"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => void clearChat()}
              className="flex items-center gap-2 rounded-xl border border-rose-400/30 bg-rose-500/15 px-4 py-2 text-sm text-rose-200 transition hover:bg-rose-500/25"
            >
              <Check className="h-4 w-4" /> Clear chat
            </button>
          </>
        }
      >
        This removes the stored messages for this report. The report itself is not affected.
      </Modal>
    </>
  );
}
