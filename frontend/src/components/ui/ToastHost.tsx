import { AnimatePresence, motion } from 'framer-motion';
import { CheckCircle2, Info, XCircle } from 'lucide-react';
import { useApp } from '../../context/AppContext';

const ICONS = {
  success: CheckCircle2,
  error: XCircle,
  info: Info,
} as const;

const STYLES = {
  success: 'border-emerald-400/30 bg-emerald-500/10 text-emerald-200',
  error: 'border-rose-400/30 bg-rose-500/10 text-rose-200',
  info: 'border-cyanx-400/30 bg-cyanx-500/10 text-cyanx-200',
} as const;

/** Toast stack - bottom centre so it never collides with Webby (bottom right). */
export default function ToastHost() {
  const { toasts, dismissToast } = useApp();

  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-6 z-[60] flex flex-col items-center gap-2 px-4">
      <AnimatePresence>
        {toasts.map((toast) => {
          const Icon = ICONS[toast.kind];
          return (
            <motion.button
              key={toast.id}
              type="button"
              onClick={() => dismissToast(toast.id)}
              initial={{ opacity: 0, y: 14, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 10, scale: 0.97 }}
              transition={{ duration: 0.22, ease: 'easeOut' }}
              className={`pointer-events-auto flex max-w-lg items-start gap-2.5 rounded-xl border px-4 py-3 text-left text-sm shadow-card backdrop-blur-md ${STYLES[toast.kind]}`}
            >
              <Icon className="mt-0.5 h-4 w-4 shrink-0" />
              <span className="text-slate-100">{toast.message}</span>
            </motion.button>
          );
        })}
      </AnimatePresence>
    </div>
  );
}
