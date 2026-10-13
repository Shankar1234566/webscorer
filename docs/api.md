# REST API — WebScorer AI

Base URL: `http://localhost:8080/api`

All responses are JSON (`text/csv` for the export endpoint). Errors always use
the same envelope:

```json
{
  "timestamp": "2026-10-13T10:41:02Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Enter a website URL to scan.",
  "path": "/api/scans"
}
```

| Status | When |
|---|---|
| `400` | validation failure (blank/oversized URL, blank question, malformed URL) |
| `404` | scan id does not exist |
| `409` | copilot question asked before the scan finished |
| `500` | unexpected server error (message is always generic) |

---

## `GET /api/health`

Backend liveness plus how the copilot is configured.

```json
{ "status": "UP", "application": "webscorer-ai",
  "timestamp": "2026-10-13T10:41:02Z",
  "aiConfigured": false, "copilotMode": "local-guidance" }
```

---

## `POST /api/scans`

Start a website scan. Returns **201** as soon as the scan is queued.

Request:

```json
{ "url": "https://example.com" }
```

Response `201` + `Location: /api/scans/{id}`:

```json
{ "id": 12, "url": "https://example.com", "status": "PENDING", "stage": null,
  "overallScore": null, "createdAt": "2026-10-13T10:41:02Z", "completedAt": null }
```

`400` for blank, over-long, malformed or non-HTTP(S) URLs. SSRF/DNS rejections
happen asynchronously and are reported through the scan's `status: FAILED`
+ `errorMessage`.

---

## `GET /api/scans`

Recent scans, newest first. Returns `ScanSummary[]`.

---

## `GET /api/scans/{id}`

Scan status (including the live `stage` while running) **and** the report.
Poll this while `status` is `PENDING | VALIDATING | FETCHING | ANALYZING | SCORING`.

```json
{
  "scan": {
    "id": 12, "url": "https://example.com", "finalUrl": "https://example.com/",
    "pageTitle": "Example Domain", "status": "COMPLETED", "stage": null,
    "overallScore": 86, "summary": "Fetched example.com with HTTP 200 in 210 ms…",
    "httpStatus": 200, "durationMs": 210,
    "checksPassed": 21, "checksFailed": 3, "checksNotRun": 1,
    "errorMessage": null, "createdAt": "…", "completedAt": "…"
  },
  "categoryScores": [
    { "category": "SEO", "label": "SEO", "hint": "Search visibility",
      "score": 86, "checksPassed": 6, "checksApplicable": 7,
      "checksNotRun": 0, "checksDefined": 8 }
  ],
  "findings": [
    { "id": 101, "checkId": "a11y.images.alt", "category": "ACCESSIBILITY",
      "severity": "HIGH", "status": "FAIL", "title": "Images have alt attributes",
      "evidence": "9 image(s): 5 with alt text, 0 decorative, 4 without alt.",
      "explanation": "Screen reader users hear alt text instead…",
      "recommendation": "Add alt text to informative images…",
      "codeExample": "<img src=… alt=…>", "weight": 1 }
  ],
  "scoringNotes": ["SEO: 6 of 7 applicable checks passed = 86/100."],
  "previousScanId": 9, "previousScore": 79, "previousScanAt": "2026-10-08T…",
  "sample": false
}
```

`status: FAILED` carries `errorMessage` and empty `categoryScores`/`findings`.

---

## `GET /api/scans/{id}/findings`

`Finding[]` for one scan (all statuses: `PASS`, `FAIL`, `NOT_APPLICABLE`, `NOT_RUN`).

## `DELETE /api/scans/{id}`

Deletes the scan with its findings, category scores and copilot messages. `204`.

---

## `GET /api/scans/{id}/export`

CSV download (`Content-Disposition: attachment`, `filename=webscorer-{host}-{id}.csv`).
Contains scan metadata, category scores and one row per check.

---

## `POST /api/scans/{id}/copilot`

Ask Webby a question about this report.

Request: `{ "question": "What should I fix first?" }` (1–2000 chars)

Response:

```json
{
  "message": "Here's the order I'd tackle things for example.com:\n\n1. …",
  "provider": "LOCAL",
  "note": "Answered from this scan's saved report using built-in rules - no AI key is configured.",
  "suggestions": ["What should I fix first?", "Explain my website score", …],
  "createdAt": "2026-10-13T10:41:05Z"
}
```

* `provider: "LOCAL"` — rule-based local guidance (no API key configured, or the
  AI provider failed; `note` then explains the fallback).
* `provider: "LLM"` — the configured AI provider answered.
* `409` if the scan is not `COMPLETED` yet.

Only report metadata is sent to an LLM — never the downloaded page body.

## `GET /api/scans/{id}/copilot`

Stored chat history for this report: `[{ "id", "role", "message", "provider", "createdAt" }]`.

## `DELETE /api/scans/{id}/copilot`

Clears the stored conversation for this report. `204`.
