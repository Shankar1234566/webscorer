import { motion, useReducedMotion } from 'framer-motion';

interface Props {
  score: number | null;
  size?: number;
  label?: string;
}

/**
 * Animated donut score. A null score renders an honest "not enough data"
 * state rather than a zero.
 */
export default function ScoreRing({ score, size = 176, label = 'Overall score' }: Props) {
  const reduceMotion = useReducedMotion();
  const radius = 70;
  const circumference = 2 * Math.PI * radius;
  const percent = score === null ? 0 : Math.max(0, Math.min(100, score));
  const stroke = circumference * (1 - percent / 100);
  const tone =
    score === null ? '#64748b' : score >= 70 ? '#22d3ee' : score >= 50 ? '#fbbf24' : '#fb7185';

  return (
    <div className="relative inline-flex items-center justify-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox="0 0 160 160" role="img" aria-label={`${label}: ${score ?? 'not enough data'}`}>
        <circle cx="80" cy="80" r={radius} fill="none" stroke="rgba(255,255,255,0.07)" strokeWidth="12" />
        <motion.circle
          cx="80"
          cy="80"
          r={radius}
          fill="none"
          stroke={tone}
          strokeWidth="12"
          strokeLinecap="round"
          strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference }}
          animate={{ strokeDashoffset: stroke }}
          transition={{ duration: reduceMotion ? 0 : 1.1, ease: [0.16, 1, 0.3, 1] }}
          transform="rotate(-90 80 80)"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        {score === null ? (
          <span className="px-6 text-center text-sm font-semibold text-slate-400">Not enough data</span>
        ) : (
          <>
            <motion.span
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.25, duration: 0.4 }}
              className="text-4xl font-bold tracking-tight text-white"
            >
              {score}
            </motion.span>
            <span className="text-xs text-slate-400">out of 100</span>
          </>
        )}
        <span className="mt-1 text-[11px] uppercase tracking-widest text-slate-500">{label}</span>
      </div>
    </div>
  );
}
