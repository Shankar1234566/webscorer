import type {
  CopilotMessage,
  CopilotResponse,
  Finding,
  HealthResponse,
  ScanDetail,
  ScanSummary,
} from '../types/api';

/**
 * Thin typed client for the Spring Boot API.
 * In dev the Vite proxy forwards /api to localhost:8080, so no CORS setup is
 * needed; set VITE_API_BASE to point a built bundle at another origin.
 */
const API_BASE = import.meta.env.VITE_API_BASE ?? '';

export class ApiRequestError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
      ...init,
    });
  } catch {
    throw new ApiRequestError(
      'Cannot reach the backend. Make sure it is running on http://localhost:8080.',
      0,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  let body: unknown = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = null;
    }
  }

  if (!response.ok) {
    const message =
      body && typeof body === 'object' && 'message' in body
        ? String((body as { message: unknown }).message)
        : response.status === 403
          ? 'The backend refused the request (403): its CORS policy does not allow this page origin.'
          : `Request failed with status ${response.status}`;
    throw new ApiRequestError(message, response.status);
  }
  return body as T;
}

export const api = {
  health: () => request<HealthResponse>('/api/health'),

  createScan: (url: string) =>
    request<ScanSummary>('/api/scans', {
      method: 'POST',
      body: JSON.stringify({ url }),
    }),

  getScan: (id: number | string) => request<ScanDetail>(`/api/scans/${id}`),

  listScans: () => request<ScanSummary[]>('/api/scans'),

  findings: (id: number | string) => request<Finding[]>(`/api/scans/${id}/findings`),

  deleteScan: (id: number | string) => request<void>(`/api/scans/${id}`, { method: 'DELETE' }),

  askCopilot: (id: number | string, question: string) =>
    request<CopilotResponse>(`/api/scans/${id}/copilot`, {
      method: 'POST',
      body: JSON.stringify({ question }),
    }),

  copilotHistory: (id: number | string) =>
    request<CopilotMessage[]>(`/api/scans/${id}/copilot`),

  clearCopilot: (id: number | string) =>
    request<void>(`/api/scans/${id}/copilot`, { method: 'DELETE' }),

  exportUrl: (id: number | string) => `${API_BASE}/api/scans/${id}/export`,
};
