import { useLocation } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import type { WebbyMood } from '../components/webby/WebbyRobot';

export interface WebbyHint {
  message: string;
  mood: WebbyMood;
  /** Show the unread dot: there is something the user should act on. */
  notify: boolean;
  ctaLabel?: string;
  /** internal navigation target for the bubble button */
  ctaHref?: string;
}

const STAGE_MESSAGES: Record<string, string> = {
  VALIDATING: 'Checking that this URL is safe to fetch…',
  FETCHING: 'Downloading the page HTML now…',
  ANALYZING: 'Running the 25 checks on the markup…',
  SCORING: 'Crunching your category scores…',
};

/**
 * Derives Webby's speech bubble from the real application state: current
 * route, live scan stage and the loaded report. No random or off-topic lines.
 */
export function useWebbyHint(): WebbyHint {
  const location = useLocation();
  const { activeScan, scanDetail } = useApp();
  const path = location.pathname;

  // 1. A scan is running - use the stage the backend actually reports.
  if (activeScan && activeScan.status !== 'COMPLETED' && activeScan.status !== 'FAILED') {
    const stage = activeScan.stage ?? 'VALIDATING';
    if (activeScan.status === 'PENDING') {
      return { message: 'Your scan is queued - starting now.', mood: 'thinking', notify: false };
    }
    return { message: STAGE_MESSAGES[stage] ?? 'Scanning…', mood: 'thinking', notify: false };
  }

  if (activeScan && activeScan.status === 'FAILED') {
    return {
      message: `The scan stopped: ${activeScan.errorMessage ?? 'unknown error'}`,
      mood: 'worried',
      notify: true,
      ctaLabel: 'Try another URL',
      ctaHref: '/scanner',
    };
  }

  // 2. A finished report is on screen.
  if (scanDetail?.scan.status === 'COMPLETED') {
    const issues = scanDetail.findings.filter((finding) => finding.status === 'FAIL');
    if (issues.length > 0) {
      return {
        message: `I found ${issues.length} issue${issues.length === 1 ? '' : 's'} worth fixing. Let's start with "${issues[0].title}".`,
        mood: 'excited',
        notify: true,
        ctaLabel: 'Show me',
        ctaHref: `/report/${scanDetail.scan.id}#finding-${issues[0].id}`,
      };
    }
    return {
      message:
        scanDetail.scan.overallScore === null
          ? 'There was not enough evidence for a score - but I can still walk you through the checks.'
          : 'Everything passed. Want me to explain how your score was calculated?',
      mood: 'happy',
      notify: false,
      ctaLabel: 'Explain my score',
    };
  }

  // 3. Route-based defaults.
  if (path.startsWith('/history')) {
    return { message: 'Your saved reports live here - compare scores across scans.', mood: 'happy', notify: false };
  }
  if (path.startsWith('/copilot')) {
    return { message: 'Ask me anything about the report you have open.', mood: 'happy', notify: false };
  }
  if (path.startsWith('/scanner')) {
    return { message: 'Paste a URL and I will help you inspect it.', mood: 'happy', notify: true, ctaLabel: 'Focus the input' };
  }
  if (path.startsWith('/report')) {
    return { message: 'Loading the report…', mood: 'thinking', notify: false };
  }

  // 4. Home.
  return {
    message: "Hi! I'm Webby. Want to check your website?",
    mood: 'happy',
    notify: true,
    ctaLabel: 'Scan a website',
    ctaHref: '/scanner',
  };
}
