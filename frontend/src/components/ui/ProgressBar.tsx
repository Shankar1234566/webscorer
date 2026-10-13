interface Props {
  label: string;
  value: number | null;
  /** When null the bar shows an "insufficient data" state instead of a number. */
  defined: number;
  applicable: number;
}

/** Horizontal, animated category progress bar. */
export default function ProgressBar({ label, value, defined, applicable }: Props) {
  const insufficient = value === null;
  const width = insufficient ? 0 : Math.max(2, Math.min(100, value));

  return (
    <div className="w-full">
      <div className="mb-1.5 flex items-baseline justify-between text-xs">
        <span className="text-slate-400">{label}</span>
        <span className="font-semibold text-slate-200">
          {insufficient ? (
            <span className="text-slate-500">Not enough data</span>
          ) : (
            <>
              {value}
              <span className="text-slate-500">/100</span>
            </>
          )}
        </span>
      </div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-white/[0.07]">
        <div
          className={`h-full rounded-full transition-[width] duration-700 ease-out ${
            insufficient
              ? 'bg-slate-600'
              : width >= 70
                ? 'bg-gradient-to-r from-cyanx-400 to-electric-500'
                : width >= 50
                  ? 'bg-gradient-to-r from-amber-400 to-orange-500'
                  : 'bg-gradient-to-r from-rose-500 to-orange-500'
          }`}
          style={{ width: `${width}%` }}
        />
      </div>
      <div className="mt-1 text-[11px] text-slate-500">
        {applicable} of {defined} checks evaluated
      </div>
    </div>
  );
}
