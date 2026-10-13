// ---------------------------------------------------------------------------
// Types mirroring the Spring Boot REST API (backend/src/main/java/.../dto)
// ---------------------------------------------------------------------------

export type ScanStatus =
  | 'PENDING'
  | 'VALIDATING'
  | 'FETCHING'
  | 'ANALYZING'
  | 'SCORING'
  | 'COMPLETED'
  | 'FAILED';

export type ScanStage = 'VALIDATING' | 'FETCHING' | 'ANALYZING' | 'SCORING';

export type Category = 'SEO' | 'ACCESSIBILITY' | 'TECHNICAL' | 'CONTENT';

export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

export type CheckStatus = 'PASS' | 'FAIL' | 'NOT_APPLICABLE' | 'NOT_RUN';

export interface ScanSummary {
  id: number;
  url: string;
  finalUrl: string | null;
  pageTitle: string | null;
  status: ScanStatus;
  stage: ScanStage | null;
  overallScore: number | null;
  summary: string | null;
  httpStatus: number | null;
  durationMs: number | null;
  checksPassed: number | null;
  checksFailed: number | null;
  checksNotRun: number | null;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface CategoryScore {
  category: Category;
  label: string;
  hint: string;
  score: number | null;
  checksPassed: number;
  checksApplicable: number;
  checksNotRun: number;
  checksDefined: number;
}

export interface Finding {
  id: number;
  checkId: string;
  category: Category;
  severity: Severity;
  status: CheckStatus;
  title: string;
  evidence: string;
  explanation: string | null;
  recommendation: string | null;
  codeExample: string | null;
  weight: number;
}

export interface ScanDetail {
  scan: ScanSummary;
  categoryScores: CategoryScore[];
  findings: Finding[];
  scoringNotes: string[];
  previousScanId: number | null;
  previousScore: number | null;
  previousScanAt: string | null;
  sample: boolean;
}

export interface CopilotResponse {
  message: string;
  provider: 'LOCAL' | 'LLM';
  note: string | null;
  suggestions: string[];
  createdAt: string;
}

export interface CopilotMessage {
  id: number;
  role: 'USER' | 'ASSISTANT';
  message: string;
  provider: string | null;
  createdAt: string;
}

export interface HealthResponse {
  status: string;
  application: string;
  timestamp: string;
  aiConfigured: boolean;
  copilotMode: 'llm' | 'local-guidance';
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string | null;
}
