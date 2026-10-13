import { Outlet, useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { useEffect } from 'react';
import CopilotChatPanel from '../webby/CopilotChatPanel';
import WebbyWidget from '../webby/WebbyWidget';
import ToastHost from '../ui/ToastHost';
import Header from './Header';

export default function AppLayout() {
  const location = useLocation();

  // Keep the browser position sane when navigating between pages.
  useEffect(() => {
    if (!window.location.hash) {
      window.scrollTo({ top: 0, behavior: 'auto' });
    }
  }, [location.pathname, location.hash]);

  return (
    <div className="flex min-h-screen flex-col bg-grid-fade">
      <Header />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 pb-40 pt-8 sm:px-6">
        <AnimatePresence mode="wait">
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            transition={{ duration: 0.25, ease: 'easeOut' }}
          >
            <Outlet />
          </motion.div>
        </AnimatePresence>
      </main>
      <ToastHost />
      <CopilotChatPanel />
      <WebbyWidget />
    </div>
  );
}
