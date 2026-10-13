# WebScorer AI 🤖

A full-stack internship prototype: scan any **public website URL**, get a
**deterministic 0–100 report** across SEO, Accessibility, Technical and Content,
and chat with **Webby**, an original animated robot copilot.

> **Honest disclaimer:** this is a learning prototype. It measures what it can
> actually see in the downloaded HTML — it never guesses, never invents scores,
> and never claims it applied a fix. It does **not** measure page speed, security
> vulnerabilities, JavaScript-rendered content or search rankings.

---

## Stack

| Layer | Tech |
|---|---|
| Frontend | React 18 · Vite · TypeScript · Tailwind CSS · Framer Motion · Lucide · Recharts · React Router · Vitest |
| Backend | Java 17 · Spring Boot 3.3 (Web, Validation, JPA) · Jsoup 1.18 · H2 (file) · JUnit 5 |
| AI | Optional LLM via `WEBSCORER_AI_API_KEY`; **rule-based local fallback** when absent |

The Java backend is the source of truth for scanning, analysis and scoring.
The frontend never computes a score.

---

## Quick start

### Prerequisites

* JDK 17+ and Maven 3.9+
* Node.js 18+ (npm)

### 1. Backend

```powershell
cd backend
mvn spring-boot:run          # http://localhost:8080
```

### 2. Frontend

```powershell
cd frontend
npm install
npm run dev                  # http://localhost:5173
```

Open <http://localhost:5173>, paste a public URL (e.g. `https://example.com`)
and press **Scan**. Progress shows the **real backend stage**
(validating → fetching → analyzing → scoring), then the report opens.

### 3. Tests

```powershell
cd backend ; mvn test        # 39 tests
cd frontend ; npm test       #  7 tests
npm run build                # tsc --noEmit + vite build
```

---

## What you get

* **Real scanner** — URL validation, SSRF guards (loopback/private/link-local/
  metadata blocked, redirects re-validated), 5 s/10 s timeouts, 1 MiB cap,
  **25 checks** over four categories.
* **Deterministic scoring** — documented in [`docs/scoring-rules.md`](docs/scoring-rules.md);
  a category with too little evidence shows **“Not enough data”** instead of a
  fake number.
* **Report dashboard** — score ring, category cards, findings with evidence and
  fix advice, score trend vs your previous scan, CSV export, history with delete.
* **Webby the copilot** — original inline SVG robot with bob/blink/tilt, a
  contextual speech bubble, mute/close, and a floating chat panel. Fully local
  guidance (clearly labelled) unless an AI key is configured. Reduced-motion is
  respected and it never blocks the UI.
* **Persistence** — H2 file database (`./data/webscorer`) for scans, findings and
  chat history; MySQL-ready profile included.

---

## Configuration

Copy `.env.example` to `.env` (frontend) / set environment variables (backend):

| Variable | Where | Default | Purpose |
|---|---|---|---|
| `WEBSCORER_AI_API_KEY` | backend | *(empty ⇒ local mode)* | enables the LLM copilot provider |
| `WEBSCORER_AI_BASE_URL` / `_MODEL` / `_PROVIDER` | backend | see `.env.example` | LLM endpoint tuning |
| `VITE_API_BASE` | frontend | `""` (dev proxy) | API origin for production builds |
| `webscorer.cors.allowed-origins` | backend | localhost:5173/4173 **+ any localhost port** | CORS allow-list |
| `webscorer.scan.*` | backend | 5000/10000 ms, 3 redirects, 1 MiB | fetch limits |

---

## Documentation

| File | Contents |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | structure, scan lifecycle, layering, data model |
| [`docs/scoring-rules.md`](docs/scoring-rules.md) | check statuses, category/overall formulas, worked example |
| [`docs/api.md`](docs/api.md) | every REST endpoint with request/response samples |
| [`docs/security.md`](docs/security.md) | SSRF defences, limits, and the honest list of what is *not* protected |

---

## Verified status (what actually works)

* ✅ Backend: **40/40** unit + integration tests (`mvn test`)
* ✅ Frontend: `npm run build` (typecheck + bundle) and **7/7** Vitest tests
* ✅ Real end-to-end scans against live public URLs (findings match the fetched HTML)
* ✅ Copilot local mode labelled `LOCAL` with an explicit “no AI key” note
* ⚠️ Limitations: see [`docs/security.md`](docs/security.md) — DNS-rebinding
  TOCTOU, no rate limiting, no performance/security audit, and the JS bundle is
  large (~775 kB) because Recharts ships unminified in dev-adjacent builds.

## Project layout

```
frontend/   React app (src/{components,pages,hooks,context,services,types})
backend/    Spring Boot app (src/main/java/com/webscorer/**)
docs/       architecture, scoring rules, API reference, security notes
.env.example  .gitignore  README.md
```
