import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import type { ScanDetail, ScanSummary } from '../types/api';

export type ToastKind = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

/**
 * Application-wide UI state: the report being viewed, the scan currently
 * running, toast notifications and Webby's mute switch.
 */
interface AppContextValue {
  scanDetail: ScanDetail | null;
  setScanDetail: (detail: ScanDetail | null) => void;
  activeScan: ScanSummary | null;
  setActiveScan: (scan: ScanSummary | null) => void;
  toasts: Toast[];
  pushToast: (message: string, kind?: ToastKind) => void;
  dismissToast: (id: number) => void;
  webbyMuted: boolean;
  setWebbyMuted: (muted: boolean) => void;
  copilotOpen: boolean;
  setCopilotOpen: (open: boolean) => void;
  /** Prefill text for the copilot input (used by "Ask Webby" buttons). */
  copilotPrefill: string | null;
  openCopilot: (prefill?: string | null) => void;
}

const AppContext = createContext<AppContextValue | null>(null);

let toastSequence = 0;

export function AppProvider({ children }: { children: ReactNode }) {
  const [scanDetail, setScanDetail] = useState<ScanDetail | null>(null);
  const [activeScan, setActiveScan] = useState<ScanSummary | null>(null);
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [webbyMuted, setWebbyMuted] = useState(false);
  const [copilotOpen, setCopilotOpen] = useState(false);
  const [copilotPrefill, setCopilotPrefill] = useState<string | null>(null);

  const dismissToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const pushToast = useCallback(
    (message: string, kind: ToastKind = 'info') => {
      const id = ++toastSequence;
      setToasts((current) => [...current, { id, kind, message }]);
      window.setTimeout(() => dismissToast(id), 5200);
    },
    [dismissToast],
  );

  const openCopilot = useCallback((prefill: string | null = null) => {
    setCopilotPrefill(prefill);
    setCopilotOpen(true);
  }, []);

  const value = useMemo<AppContextValue>(
    () => ({
      scanDetail,
      setScanDetail,
      activeScan,
      setActiveScan,
      toasts,
      pushToast,
      dismissToast,
      webbyMuted,
      setWebbyMuted,
      copilotOpen,
      setCopilotOpen,
      copilotPrefill,
      openCopilot,
    }),
    [
      scanDetail,
      activeScan,
      toasts,
      pushToast,
      dismissToast,
      webbyMuted,
      copilotOpen,
      copilotPrefill,
      openCopilot,
    ],
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp(): AppContextValue {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used inside <AppProvider>');
  }
  return context;
}
