# WebScorer AI — Scoring Rules

The scoring engine is **deterministic**: the same findings always produce the same
numbers. No language model is involved anywhere in scoring
(`com.webscorer.service.ScoringService`).

## 1. Checks

Every check lives in the catalog `com.webscorer.analysis.CheckCatalog` (25 checks).
A check definition carries:

| Field | Meaning |
|---|---|
| `id` | stable identifier, e.g. `seo.title.present` |
| `category` | `SEO`, `ACCESSIBILITY`, `TECHNICAL`, `CONTENT` |
| `severity` | severity used **when the check fails** |
| `weight` | contribution to the category score (all checks are `1` today) |
| `title`, `whyItMatters`, `howToFix`, `codeExample` | copy shown in the report |

## 2. Check statuses

| Status | Counts toward score? | Meaning |
|---|---|---|
| `PASS` | yes (numerator + denominator) | the check ran and the page satisfies it |
| `FAIL` | yes (denominator only) | the check ran and the page violates it |
| `NOT_APPLICABLE` | no | the check ran but does not apply (e.g. no `<iframe>` on the page) |
| `NOT_RUN` | no | the check could **not** be executed — missing evidence, never a failure |

> A missing check is deliberately different from a failed check. Missing evidence
> never lowers a score.

## 3. Category score

For each category:

```
applicableWeight = Σ weight of checks with status PASS or FAIL
passedWeight     = Σ weight of checks with status PASS

categoryScore = round(100 × passedWeight / applicableWeight)
```

A category reports **"Not enough data"** (`score = null`) when fewer than
`max(2, ceil(0.6 × definedChecks))` checks could be evaluated:

| Category | Defined checks | Minimum evaluated |
|---|---|---|
| SEO | 8 | 5 |
| Accessibility | 6 | 4 |
| Technical | 7 | 5 |
| Content | 4 | 3 |

## 4. Overall score

```
overallScore = round( Σ (categoryScore × categoryWeight) / Σ categoryWeight )
```

over the categories that have a score, with these weights:

| Category | Weight |
|---|---|
| SEO | 0.35 |
| Accessibility | 0.25 |
| Technical | 0.25 |
| Content | 0.15 |

The overall score is **withheld** (shown as "Not enough data") when:

* fewer than two categories have a score, or
* the combined weight of scored categories is below `0.50`.

## 5. Worked example

A page where SEO scores 5/8, Accessibility 6/6, Technical 7/7 and Content 4/4:

* SEO = `round(100 × 5/8)` = **63**
* Accessibility = **100**, Technical = **100**, Content = **100**
* Overall = `(63×0.35 + 100×0.25 + 100×0.25 + 100×0.15) / 1.00` = **87**

Re-running the scan against unchanged HTML reproduces the same numbers; this is
covered by `ScoringServiceTest.scoringIsDeterministicForTheSameResults`.

## 6. Priorities

Findings in the report and the "What should I fix first?" copilot answer are
sorted by:

1. severity rank (`CRITICAL > HIGH > MEDIUM > LOW > INFO`), then
2. category weight (SEO first), then
3. title (stable ordering).

## 7. What is *not* scored

The scanner never claims to measure what it did not measure:

* page speed / Core Web Vitals (needs PageSpeed Insights or similar, not configured),
* security vulnerabilities or penetration testing,
* JavaScript-rendered content (only the downloaded HTML is inspected),
* search ranking outcomes.

Those areas answer "not measured" in both the report and the copilot.
