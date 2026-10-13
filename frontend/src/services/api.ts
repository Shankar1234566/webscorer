
import type {
  CopilotMessage,
  CopilotResponse,
  Finding,
  HealthResponse,
  ScanDetail,
  ScanSummary,
} from '../types/api';

/**
 * Typed client for the Spring Boot API.
 * In development, Vite can proxy /api to localhost:8080.
 * In production, requests go directly to the Render backend.
 */
const API_BASE = 'https://webscorerai.onrender.com';

export class ApiRequestError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
  }
}

async function request<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(init?.headers ?? {}),
      },
    });
  } catch {
    throw new ApiRequestError(
      'Cannot reach the backend. Please check your internet connection or backend availability.',
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
      body &&
      typeof body === 'object' &&
      'message' in body
        ? String((body as { message: unknown }).message)
        : response.status === 403
          ? 'The backend refused the request (403). Check the backend CORS configuration.'
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

  getScan: (id: number | string) =>
    request<ScanDetail>(`/api/scans/${id}`),

  listScans: () =>
    request<ScanSummary[]>('/api/scans'),

  findings: (id: number | string) =>
    request<Finding[]>(`/api/scans/${id}/findings`),

  deleteScan: (id: number | string) =>
    request<void>(`/api/scans/${id}`, {
      method: 'DELETE',
    }),

  askCopilot: (id: number | string, question: string) =>
    request<CopilotResponse>(`/api/scans/${id}/copilot`, {
      method: 'POST',
      body: JSON.stringify({ question }),
    }),

  copilotHistory: (id: number | string) =>
    request<CopilotMessage[]>(`/api/scans/${id}/copilot`),

  clearCopilot: (id: number | string) =>
    request<void>(`/api/scans/${id}/copilot`, {
      method: 'DELETE',
    }),

  exportUrl: (id: number | string) =>
    `${API_BASE}/api/scans/${id}/export`,
};
