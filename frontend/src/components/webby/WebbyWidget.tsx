import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { Volume2, VolumeX, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import { useWebbyHint } from '../../hooks/useWebbyHint';
import WebbyRobot from './WebbyRobot';

/**
 * Floating Webby: gentle bob + tilt, occasional blink, contextual speech
 * bubble with close and mute controls, and a notification dot when there is
 * something worth acting on. Clicking the robot opens the copilot panel.
 */
export default function WebbyWidget() {
  const hint = useWebbyHint();
  const navigate = useNavigate();
  const location = useLocation();
  const reduceMotion = useReducedMotion();
  const { webbyMuted, setWebbyMuted, copilotOpen, openCopilot } = useApp();
  const [bubbleOpen, setBubbleOpen] = useState(true);
  const [wave, setWave] = useState(false);

  // Re-show the bubble whenever Webby has something new to say.
  useEffect(() => {
    setBubbleOpen(!webbyMuted);
  }, [hint.message, webbyMuted]);

  const handleRobotClick = () => {
    setWave(true);
    window.setTimeout(() => setWave(false), 900);
    if (!copilotOpen) {
      openCopilot(null);
    }
  };

  const handleCta = () => {
    if (hint.ctaHref) {
      const [path, hash] = hint.ctaHref.split('#');
      navigate(hash ? `${path}#${hash}` : path);
      if (hash) {
        window.setTimeout(() => {
          document.getElementById(`finding-${hash}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 350);
      }
      return;
    }
    if (location.pathname === '/scanner') {
      document.getElementById('url-input')?.focus();
      return;
    }
    openCopilot(hint.ctaLabel ?? null);
  };

  const animated = !webbyMuted && !reduceMotion;

  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-50 flex flex-col items-end gap-3 sm:bottom-6 sm:right-6">
      <AnimatePresence>
        {bubbleOpen && !copilotOpen && (
          <motion.div
            key={hint.message}
            initial={{ opacity: 0, y: 10, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 6, scale: 0.97 }}
            transition={{ duration: 0.25, ease: 'easeOut' }}
            className="pointer-events-auto relative max-w-[min(20rem,calc(100vw-6rem))] rounded-2xl border border-white/10 bg-navy-850/95 p-3.5 shadow-card backdrop-blur-md"
          >
            <div className="flex items-start gap-2">
              <p className="flex-1 text-[13px] leading-relaxed text-slate-200">{hint.message}</p>
              <div className="flex shrink-0 gap-1">
                <button
                  type="button"
                  aria-label={webbyMuted ? 'Unmute Webby' : 'Mute Webby'}
                  title={webbyMuted ? 'Unmute Webby' : 'Pause Webby animations and messages'}
                  onClick={() => setWebbyMuted(!webbyMuted)}
                  className="rounded-md p-1 text-slate-500 transition hover:bg-white/10 hover:text-cyanx-300"
                >
                  {webbyMuted ? <VolumeX className="h-3.5 w-3.5" /> : <Volume2 className="h-3.5 w-3.5" />}
                </button>
                <button
                  type="button"
                  aria-label="Close Webby message"
                  onClick={() => setBubbleOpen(false)}
                  className="rounded-md p-1 text-slate-500 transition hover:bg-white/10 hover:text-white"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
            {hint.ctaLabel && (
              <button
                type="button"
                onClick={handleCta}
                className="mt-2.5 w-full rounded-lg border border-cyanx-400/30 bg-cyanx-500/10 px-3 py-1.5 text-xs font-medium text-cyanx-200 transition hover:bg-cyanx-500/20"
              >
                {hint.ctaLabel}
              </button>
            )}
            <span className="absolute -bottom-2 right-6 h-4 w-4 rotate-45 border-b border-r border-white/10 bg-navy-850" />
          </motion.div>
        )}
      </AnimatePresence>

      <motion.button
        type="button"
        onClick={handleRobotClick}
        aria-label="Open Webby, the WebScorer copilot"
        className="pointer-events-auto relative rounded-full outline-none"
        initial={false}
        animate={
          animated
            ? {
                y: [0, -8, 0],
                rotate: wave ? [0, -8, 8, -4, 0] : [-2.5, 2.5, -2.5],
              }
            : { y: 0, rotate: 0 }
        }
        transition={
          animated
            ? {
                y: { duration: 4.2, repeat: Infinity, ease: 'easeInOut' },
                rotate: wave
                  ? { duration: 0.9, ease: 'easeInOut' }
                  : { duration: 7, repeat: Infinity, ease: 'easeInOut' },
              }
            : { duration: 0.2 }
        }
        whileTap={{ scale: 0.94 }}
      >
        <span className="absolute -right-0.5 -top-0.5 z-10 flex h-5 w-5 items-center justify-center rounded-full border-2 border-navy-950 bg-cyanx-400 text-[10px] font-bold text-navy-950">
          !
        </span>
        <span className="flex h-[74px] w-[74px] items-center justify-center rounded-full border border-white/10 bg-gradient-to-b from-navy-800 to-navy-900 shadow-glow sm:h-20 sm:w-20">
          <WebbyRobot mood={hint.mood} size={62} />
        </span>
      </motion.button>
    </div>
  );
}
