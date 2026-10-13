import { useState } from 'react';
import type { ReactNode } from 'react';
import { Info } from 'lucide-react';

/** Small hover/focus tooltip used for metrics that need a definition. */
export default function Tooltip({ label, children }: { label: string; children?: ReactNode }) {
  const [open, setOpen] = useState(false);

  return (
    <span
      className="relative inline-flex"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      onFocus={() => setOpen(true)}
      onBlur={() => setOpen(false)}
    >
      {children}
      <button
        type="button"
        aria-label="What does this mean?"
        className="ml-1.5 inline-flex h-4 w-4 items-center justify-center rounded-full text-slate-500 transition hover:text-cyanx-300"
        onClick={() => setOpen((value) => !value)}
      >
        <Info className="h-3.5 w-3.5" />
      </button>
      {open && (
        <span
          role="tooltip"
          className="absolute bottom-full left-0 z-50 mb-2 w-64 rounded-lg border border-white/10 bg-navy-800 px-3 py-2 text-left text-xs leading-relaxed text-slate-300 shadow-card"
        >
          {label}
        </span>
      )}
    </span>
  );
}
