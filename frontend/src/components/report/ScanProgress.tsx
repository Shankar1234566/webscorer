import { motion } from 'framer-motion';
import { Check, Loader2 } from 'lucide-react';
import type { ScanStage, ScanStatus } from '../../types/api';

const STAGES: { key: ScanStage; label: string; hint: string }[] = [
  { key: 'VALIDATING', label: 'Validating the URL', hint: 'Scheme, DNS and SSRF guards' },
  { key: 'FETCHING', label: 'Downloading the HTML', hint: 'Public page only, size and time limited' },
  { key: 'ANALYZING', label: 'Running the checks', hint: 'SEO, accessibility, technical, content' },
  { key: 'SCORING', label: 'Calculating scores', hint: 'Deterministic, rule-based' },
];

const STAGE_INDEX: Record<ScanStage, number> = {
  VALIDATING: 0,
  FETCHING: 1,
  ANALYZING: 2,
  SCORING: 3,
};

interface Props {
  status: ScanStatus;
  stage: ScanStage | null;
  url: string;
}

/**
 * Real progress: every row reflects a stage the backend is actually in.
 * No fabricated percentage - the scan reports discrete stages, so that is
 * what the user sees.
 */
export default function ScanProgress({ status, stage, url }: Props) {
  const currentIndex = status === 'PENDING' || !stage ? -1 : STAGE_INDEX[stage];

  return (
    <div className="mx-auto max-w-2xl">
      <div className="card overflow-hidden">
        <div className="border-b border-white/[0.07] bg-white/[0.03] px-6 py-5">
          <div className="flex items-center gap-3">
            <motion.span
              animate={{ rotate: currentIndex >= 0 ? 360 : 0 }}
              transition={{ duration: 2.4, repeat: currentIndex >= 0 ? Infinity : 0, ease: 'linear' }}
              className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyanx-500/15 text-cyanx-300"
            >
              <Loader2 className="h-5 w-5" />
            </motion.span>
            <div className="min-w-0">
              <h2 className="text-lg font-semibold text-white">Scanning your website…</h2>
              <p className="truncate text-sm text-slate-400">{url}</p>
            </div>
          </div>
        </div>

        <ol className="divide-y divide-white/[0.05]">
          {STAGES.map((stageItem, index) => {
            const done = index < currentIndex || status === 'COMPLETED';
            const active = index === currentIndex && status !== 'COMPLETED';
            return (
              <li key={stageItem.key} className="flex items-start gap-3 px-6 py-4">
                <span
                  className={`mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border text-[11px] ${
                    done
                      ? 'border-cyanx-400/40 bg-cyanx-500/15 text-cyanx-300'
                      : active
                        ? 'border-electric-400/50 bg-electric-500/15 text-electric-400'
                        : 'border-white/10 bg-white/[0.03] text-slate-500'
                  }`}
                >
                  {done ? <Check className="h-3.5 w-3.5" /> : index + 1}
                </span>
                <div className="min-w-0 flex-1">
                  <p className={`text-sm ${active ? 'font-medium text-white' : done ? 'text-slate-200' : 'text-slate-500'}`}>
                    {stageItem.label}
                  </p>
                  <p className="text-xs text-slate-500">{stageItem.hint}</p>
                </div>
                {active && (
                  <span className="mt-1 h-2 w-2 shrink-0 animate-pulse rounded-full bg-electric-400" />
                )}
              </li>
            );
          })}
        </ol>

        <div className="border-t border-white/[0.07] px-6 py-4 text-xs text-slate-500">
          Stages are read from the backend as they happen. The scan usually finishes in a few seconds.
        </div>
      </div>
    </div>
  );
}
