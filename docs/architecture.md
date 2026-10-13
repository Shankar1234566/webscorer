# Architecture — WebScorer AI

```
webscorer-ai/
├── frontend/          React 18 + Vite + TypeScript + Tailwind + Framer Motion
│   └── src/
│       ├── components/{layout,report,scanner,ui,webby}
│       ├── context/AppContext.tsx      toasts, current report, Webby state
│       ├── hooks/{useScanPolling,useWebbyHint}
│       ├── pages/{Home,Scanner,Report,History,Copilot,NotFound}
│       ├── services/{api,sampleReport}
│       └── types/api.ts                mirrors the backend DTOs
├── backend/           Spring Boot 3.3 (Java 17) + Jsoup + H2 + JPA
│   └── src/main/java/com/webscorer/
│       ├── config/       CORS, scan properties, AI properties, executor, startup cleanup
│       ├── controller/   ScanController, CopilotController, HealthController
│       ├── service/      scan orchestration, SSRF guard, fetch, scoring, copilot, report
│       ├── analysis/     check catalog + pure HTML analyzer
│       ├── repository/   Spring Data JPA
│       ├── entity/       scans, scan_findings, category_scores, copilot_messages
│       ├── dto/          request/response records
│       └── exception/    shared JSON error envelope
├── docs/              architecture.md, scoring-rules.md, api.md, security.md
├── .env.example
└── README.md
```

## Scan lifecycle

```
POST /api/scans ──► Scan row (PENDING) ──► bounded executor (2–4 threads)
                                              │
                    stage = VALIDATING ───────┤ UrlSafetyService  (scheme, DNS, SSRF)
                    stage = FETCHING ─────────┤ HtmlFetchService  (manual redirects, size/time caps)
                    stage = ANALYZING ────────┤ HtmlAnalyzer      (25 pure checks)
                    stage = SCORING ──────────┤ ScoringService    (deterministic)
                                              ▼
                              findings + category scores persisted
                              status = COMPLETED (or FAILED + errorMessage)
```

* The frontend polls `GET /api/scans/{id}` every 700 ms and renders **the stage
  the backend reports** — no fabricated percentages.
* Scans interrupted by a restart are marked `FAILED` at boot by
  `StaleScanCleanup`, instead of hanging forever.
* `GET /api/scans/{id}` returns the full report payload, so polling and the
  report page use one endpoint.

## Layering

| Layer | Responsibility | Never does |
|---|---|---|
| `HtmlAnalyzer` | pure functions over an already-downloaded `Document` | network, clock, randomness |
| `ScoringService` | weights and thresholds | I/O, LLM calls |
| `UrlSafetyService` / `HtmlFetchService` | every byte that enters the app | parse/analyse |
| `CopilotService` (interface) | answer a question from `CopilotContext` | touch the network directly (only `LlmCopilotService` does) |
| Controllers | HTTP mapping + validation | business rules |

`CopilotContext` contains **report metadata only** (scores, findings, evidence).
The downloaded page body is never sent to an AI provider.

## Copilot provider selection

```
CopilotRouter.answer()
 ├── no WEBSCORER_AI_API_KEY ──► LocalCopilotService   (provider "LOCAL")
 └── key configured ───────────► LlmCopilotService     (provider "LLM")
        └── on any failure ────► LocalCopilotService + note explaining the fallback
```

Every reply is stored in `copilot_messages` with its provider, so history always
shows who answered.

## Frontend state

* `AppContext` — the report currently open, the scan currently running, toasts,
  Webby's mute switch and the copilot panel (including its prefill text).
* `useScanPolling(id)` — drives progress + report loading from one endpoint.
* `useWebbyHint()` — derives Webby's speech bubble from the route, the live scan
  stage and the loaded report, so messages always match what is on screen.

## Data model

| Table | Purpose |
|---|---|
| `scans` | url, status, stage, overall score, summary, HTTP status, duration, counts, timestamps |
| `scan_findings` | one row per executed check (including passes), with evidence + fix |
| `category_scores` | per-category score and check counts |
| `copilot_messages` | chat history, role + provider |

H2 runs in file mode (`./data/webscorer`) so history survives restarts. Switching
to MySQL means setting `SPRING_PROFILES_ACTIVE=mysql` and the `DB_*` variables —
the entities do not change (see `application-mysql.properties`).
