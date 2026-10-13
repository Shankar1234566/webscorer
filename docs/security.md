# Security & Reliability Notes

A public URL scanner is a server-side request forgery (SSRF) primitive by
default. WebScorer AI treats blocking that as a feature, not an afterthought.

## What is enforced (server side)

`UrlSafetyService.validate()` runs **before every connection**, including one
re-validation per redirect hop in `HtmlFetchService`:

1. **Protocol allow-list** — only `http:` / `https:`. `file:`, `ftp:`,
   `javascript:` etc. are rejected (a bare host gets `https://` prepended first,
   but only when it carries no scheme at all).
2. **No credentials in URLs** — `user:pass@host` is rejected.
3. **Blocked host names** (before any DNS lookup): `localhost`, `*.localhost`,
   `*.local`, `*.internal`, `*.localdomain`, `*.home.arpa`, `metadata.google.internal`,
   `metadata`, `instance-data`, `0.0.0.0`.
4. **DNS resolution + address checks** — every resolved address must be public.
   Rejected: loopback, any-local (`0.0.0.0/8`, `::`), link-local (`169.254.0.0/16`
   incl. the cloud metadata address `169.254.169.254`, `fe80::/10`), private
   RFC1918 ranges, CGNAT `100.64.0.0/10` (covers `100.100.100.200`), multicast,
   benchmark/documentation ranges, `240.0.0.0/4` + broadcast, IPv6 unique-local
   `fc00::/7`, deprecated `fec0::/10`, and IPv4-mapped IPv6 addresses
   (`::ffff:127.0.0.1`).
5. **Manual redirect handling** — Jsoup's automatic redirect following is
   disabled. Each `Location` header is resolved and re-validated, with a
   maximum of `webscorer.scan.max-redirects` (default 3).
6. **Transfer limits** — connect/read timeout (`webscorer.scan.*-timeout-ms`,
   defaults 5 s / 10 s; Jsoup applies one combined timeout, so the larger of the
   two is used) and a response cap of `webscorer.scan.max-bytes` (default 1 MiB
   via `maxBodySize`).
7. **Content-type check** — non-HTML/XML responses are refused with a clear
   message instead of being parsed.
8. **HTTP status handling** — 4xx/5xx abort the scan and are recorded; the
   status code and duration are always stored on the scan row.
9. **No script execution** — markup is parsed with Jsoup only. The fetched page's
   JavaScript never runs, and the app never executes anything it downloads.
10. **Error hygiene** — user-facing error messages are sanitised (single line,
    length-capped) and unexpected exceptions return a generic 500 body.
11. **AI key isolation** — `WEBSCORER_AI_API_KEY` is read only by the JVM. The
    React bundle contains no secrets, and only report metadata (never the page
    body) is sent to an AI provider.
12. **No intrusive testing** — the scanner performs one GET of one public page.
    No port scanning, no form submission, no fuzzing, no auth attempts.

## Known limitations (honest list)

* **DNS rebinding (TOCTOU).** DNS is validated immediately before connecting;
  a hostile authoritative server that answers the validation lookup with a
  public IP and the connection lookup with an internal one can still win the
  race. Full prevention needs address-pinning sockets, which Jsoup does not
  expose. Use a network policy (deny egress to RFC1918/metadata) for defence in
  depth.
* **Redirect chains are re-validated per hop, not infinitely.** The hop limit is
  configurable; a chain longer than the limit fails the scan by design.
* **Response size is truncated, not rejected.** `maxBodySize` stops reading after
  the cap, so very large pages are analysed partially; the report records the
  number of bytes actually read.
* **No authentication/authorisation.** The app has no user accounts — run it on
  localhost or behind your own auth layer before exposing it.
* **No rate limiting** on `POST /api/scans`. The scan executor is bounded
  (2 core / 4 max / 16 queue) so a burst degrades to queued work rather than
  unbounded concurrency, but a public deployment should add throttling.
* **Not production-secure by claim.** There has been no professional security
  review or penetration test. Treat this as an internship-quality prototype.

## Configuration reference

| Property | Default | Meaning |
|---|---|---|
| `webscorer.scan.connect-timeout-ms` | 5000 | socket connect timeout |
| `webscorer.scan.read-timeout-ms` | 10000 | socket read timeout |
| `webscorer.scan.max-redirects` | 3 | redirect hops before failing |
| `webscorer.scan.max-bytes` | 1048576 | max downloaded body bytes |
| `webscorer.cors.allowed-origins` | `http://localhost:5173,…` | CORS allow-list for `/api/**`; any `localhost`/`127.0.0.1` port is additionally allowed as a dev origin (Vite may pick 5174+), and rejected origins get a JSON explanation |
| `webscorer.ai.*` | see `.env.example` | optional LLM provider; empty key ⇒ local mode |
