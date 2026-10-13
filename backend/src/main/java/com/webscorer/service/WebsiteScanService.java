package com.webscorer.service;

import com.webscorer.analysis.CheckResult;
import com.webscorer.analysis.HtmlAnalyzer;
import com.webscorer.analysis.PageContext;
import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import com.webscorer.domain.ScanStage;
import com.webscorer.domain.ScanStatus;
import com.webscorer.domain.Severity;
import com.webscorer.dto.CategoryScoreResponse;
import com.webscorer.dto.FindingResponse;
import com.webscorer.dto.ScanDetailResponse;
import com.webscorer.dto.ScanSummaryResponse;
import com.webscorer.entity.CategoryScore;
import com.webscorer.entity.Scan;
import com.webscorer.entity.ScanFinding;
import com.webscorer.exception.NotFoundException;
import com.webscorer.repository.CategoryScoreRepository;
import com.webscorer.repository.CopilotMessageRepository;
import com.webscorer.repository.ScanFindingRepository;
import com.webscorer.repository.ScanRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates a scan: validate -> fetch -> analyse -> score -> persist.
 *
 * <p>{@link #createScan(String)} returns as soon as the scan row exists; the
 * heavy work runs on a bounded executor and reports its <em>real</em> stage
 * through the database, so the UI never has to invent a progress percentage.</p>
 */
@Service
public class WebsiteScanService {

    private static final Logger log = LoggerFactory.getLogger(WebsiteScanService.class);

    /** Report order: severity first, then category weight, then title (stable). */
    private static final Comparator<FindingResponse> FINDING_ORDER =
            Comparator.comparingInt((FindingResponse f) -> f.severity().getRank()).reversed()
                    .thenComparingInt(f -> -(int) Math.round(
                            ScoringService.CATEGORY_WEIGHTS.getOrDefault(f.category(), 0.0d) * 100))
                    .thenComparing(FindingResponse::title);

    private final ScanRepository scanRepository;
    private final ScanFindingRepository findingRepository;
    private final CategoryScoreRepository scoreRepository;
    private final CopilotMessageRepository copilotMessageRepository;
    private final UrlSafetyService urlSafety;
    private final HtmlFetchService fetchService;
    private final HtmlAnalyzer analyzer = new HtmlAnalyzer();
    private final ScoringService scoringService;
    private final Executor scanExecutor;

    public WebsiteScanService(ScanRepository scanRepository,
                              ScanFindingRepository findingRepository,
                              CategoryScoreRepository scoreRepository,
                              CopilotMessageRepository copilotMessageRepository,
                              UrlSafetyService urlSafety,
                              HtmlFetchService fetchService,
                              ScoringService scoringService,
                              @Qualifier("scanExecutor") Executor scanExecutor) {
        this.scanRepository = scanRepository;
        this.findingRepository = findingRepository;
        this.scoreRepository = scoreRepository;
        this.copilotMessageRepository = copilotMessageRepository;
        this.urlSafety = urlSafety;
        this.fetchService = fetchService;
        this.scoringService = scoringService;
        this.scanExecutor = scanExecutor;
    }

    /** Fast, purely syntactic validation so bad input fails with a 400. */
    public String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("Enter a website URL to scan.");
        }
        String value = rawUrl.trim();
        if (value.length() > 2048) {
            throw new IllegalArgumentException("The URL must be at most 2048 characters.");
        }
        boolean hasScheme = value.matches("^[a-zA-Z][a-zA-Z0-9+.\\-]*://.*");
        if (!hasScheme) {
            value = "https://" + value;
        } else {
            String scheme = value.substring(0, value.indexOf("://")).toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("Only http:// and https:// URLs can be scanned.");
            }
        }
        URI parsed;
        try {
            parsed = URI.create(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("That is not a valid URL, for example https://example.com");
        }
        if (parsed.getHost() == null || parsed.getHost().isBlank()) {
            throw new IllegalArgumentException("That is not a valid URL, for example https://example.com");
        }
        return value;
    }

    /** Creates the row, queues the work and returns immediately. */
    public ScanSummaryResponse createScan(String rawUrl) {
        String url = normalizeUrl(rawUrl);

        Scan scan = new Scan();
        scan.setUrl(url);
        scan.setStatus(ScanStatus.PENDING);
        scan.setCreatedAt(Instant.now());
        Scan saved = scanRepository.save(scan);

        Long scanId = saved.getId();
        scanExecutor.execute(() -> runScan(scanId));
        return ScanSummaryResponse.from(saved);
    }

    private void runScan(Long scanId) {
        Scan scan = scanRepository.findById(scanId).orElse(null);
        if (scan == null) {
            return;
        }
        try {
            updateStage(scan, ScanStatus.VALIDATING, ScanStage.VALIDATING);
            URI uri = urlSafety.validate(scan.getUrl());

            updateStage(scan, ScanStatus.FETCHING, ScanStage.FETCHING);
            FetchedPage page = fetchService.fetch(uri);
            scan.setHttpStatus(page.status());
            scan.setDurationMs(page.durationMs());
            scan.setFinalUrl(page.finalUrl());

            updateStage(scan, ScanStatus.ANALYZING, ScanStage.ANALYZING);
            PageContext context = new PageContext(uri, page.doc(), page.finalUrl(), page.status(),
                    page.durationMs(), page.bytes());
            List<CheckResult> results = analyzer.analyze(context);

            updateStage(scan, ScanStatus.SCORING, ScanStage.SCORING);
            ScoreOutcome outcome = scoringService.score(results);

            persistResults(scan, results, outcome, page);
            scan.setStatus(ScanStatus.COMPLETED);
            scan.setStage(null);
            scan.setCompletedAt(Instant.now());
            scan.setOverallScore(outcome.overallScore());
            scan.setChecksPassed(outcome.checksPassed());
            scan.setChecksFailed(outcome.checksFailed());
            scan.setChecksNotRun(outcome.checksNotRun());
            scan.setSummary(buildSummary(scan, results, outcome));
            scan.setErrorMessage(null);
            scanRepository.save(scan);
            log.info("Scan {} completed for {} with score {}", scanId, scan.getUrl(), outcome.overallScore());
        } catch (Exception e) {
            log.warn("Scan {} failed: {}", scanId, e.getMessage());
            scan.setStatus(ScanStatus.FAILED);
            scan.setStage(null);
            scan.setCompletedAt(Instant.now());
            scan.setErrorMessage(safeError(e));
            scanRepository.save(scan);
        }
    }

    private void updateStage(Scan scan, ScanStatus status, ScanStage stage) {
        scan.setStatus(status);
        scan.setStage(stage);
        scanRepository.save(scan);
    }

    private void persistResults(Scan scan, List<CheckResult> results, ScoreOutcome outcome, FetchedPage page) {
        Long scanId = scan.getId();
        findingRepository.deleteByScanId(scanId);
        scoreRepository.deleteByScanId(scanId);

        List<ScanFinding> findings = new ArrayList<>(results.size());
        for (CheckResult result : results) {
            ScanFinding finding = new ScanFinding();
            finding.setScanId(scanId);
            finding.setCheckId(result.checkId());
            finding.setCategory(result.category());
            finding.setSeverity(result.severity());
            finding.setStatus(result.status());
            finding.setTitle(result.title());
            finding.setEvidence(truncate(result.evidence(), 2000));
            finding.setExplanation(truncate(result.explanation(), 2000));
            finding.setRecommendation(truncate(result.recommendation(), 2000));
            finding.setCodeExample(result.codeExample());
            finding.setWeight(result.weight());
            findings.add(finding);
        }
        findingRepository.saveAll(findings);

        List<CategoryScore> scores = new ArrayList<>();
        for (Category category : Category.values()) {
            ScoreOutcome.CategoryScoreValue value = outcome.categoryScores().get(category);
            if (value == null) {
                continue;
            }
            CategoryScore score = new CategoryScore();
            score.setScanId(scanId);
            score.setCategory(category);
            score.setScore(value.score());
            score.setChecksPassed(value.passed());
            score.setChecksApplicable(value.applicable());
            score.setChecksNotRun(value.notRun());
            scores.add(score);
        }
        scoreRepository.saveAll(scores);
        scan.setPageTitle(derivePageTitle(page));
    }

    private String derivePageTitle(FetchedPage page) {
        String title = page.doc().title();
        return title == null || title.isBlank() ? null : truncate(title.trim(), 512);
    }

    /** Evidence-based summary; generated only from what the scan actually found. */
    private String buildSummary(Scan scan, List<CheckResult> results, ScoreOutcome outcome) {
        long issues = results.stream().filter(CheckResult::isIssue).count();
        StringBuilder builder = new StringBuilder();
        builder.append("Fetched ").append(hostOf(scan.getUrl()))
                .append(" with HTTP ").append(scan.getHttpStatus())
                .append(" in ").append(scan.getDurationMs()).append(" ms.");

        if (outcome.overallScore() != null) {
            builder.append(" Overall score ").append(outcome.overallScore()).append("/100 (");
            List<String> parts = new ArrayList<>();
            for (Category category : Category.values()) {
                ScoreOutcome.CategoryScoreValue value = outcome.categoryScores().get(category);
                if (value != null && value.score() != null) {
                    parts.add(category.getLabel() + " " + value.score());
                }
            }
            builder.append(String.join(", ", parts)).append(").");
        } else {
            builder.append(" Not enough evidence was gathered to calculate an overall score.");
        }

        builder.append(" ").append(outcome.checksPassed()).append(" of ")
                .append(outcome.checksPassed() + outcome.checksFailed())
                .append(" applicable checks passed");

        Optional<CheckResult> top = topIssue(results);
        if (top.isPresent()) {
            builder.append("; highest-priority issue: ").append(top.get().title())
                    .append(" (").append(top.get().severity()).append(", ")
                    .append(top.get().category().getLabel()).append(").");
        } else if (issues == 0) {
            builder.append("; no issues were detected by these checks.");
        }
        return truncate(builder.toString(), 2000);
    }

    public Optional<CheckResult> topIssue(List<CheckResult> results) {
        return results.stream()
                .filter(CheckResult::isIssue)
                .max(Comparator.comparingInt((CheckResult r) -> r.severity().getRank())
                        .thenComparingInt(r -> (int) Math.round(
                                ScoringService.CATEGORY_WEIGHTS.getOrDefault(r.category(), 0.0d) * 100)));
    }

    // ------------------------------------------------------------- reads

    public Scan requireScan(Long id) {
        return scanRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Scan " + id + " was not found."));
    }

    public ScanSummaryResponse getSummary(Long id) {
        return ScanSummaryResponse.from(requireScan(id));
    }

    public List<ScanSummaryResponse> listScans() {
        return scanRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ScanSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScanDetailResponse getDetail(Long id) {
        Scan scan = requireScan(id);
        if (scan.getStatus() != ScanStatus.COMPLETED) {
            return new ScanDetailResponse(ScanSummaryResponse.from(scan), List.of(), List.of(), List.of(), null, null, null, false);
        }
        List<CategoryScoreResponse> scores = scoreRepository.findByScanId(id).stream()
                .map(score -> CategoryScoreResponse.from(score,
                        com.webscorer.analysis.CheckCatalog.definedCount(score.getCategory())))
                .sorted(Comparator.comparing(score -> score.category().ordinal()))
                .toList();
        List<FindingResponse> findings = orderedFindings(id);

        Long previousId = null;
        Integer previousScore = null;
        Instant previousAt = null;
        List<Scan> sameUrl = scanRepository.findTop2ByUrlOrderByCreatedAtDesc(scan.getUrl());
        for (Scan candidate : sameUrl) {
            if (!candidate.getId().equals(id) && candidate.getStatus() == ScanStatus.COMPLETED) {
                previousId = candidate.getId();
                previousScore = candidate.getOverallScore();
                previousAt = candidate.getCompletedAt();
                break;
            }
        }

        return new ScanDetailResponse(
                ScanSummaryResponse.from(scan),
                scores,
                findings,
                scoringNotes(findings, scores),
                previousId,
                previousScore,
                previousAt,
                false);
    }

    public List<FindingResponse> findings(Long id) {
        requireScan(id);
        return orderedFindings(id);
    }

    /**
     * Findings in the documented report order: severity first (CRITICAL to INFO),
     * then the category weight (SEO carries the most), then title for stability.
     */
    private List<FindingResponse> orderedFindings(Long id) {
        return findingRepository.findByScanIdOrderByCategoryAscIdAsc(id).stream()
                .map(FindingResponse::from)
                .sorted(FINDING_ORDER)
                .toList();
    }

    /** Removes a scan and everything that belongs to it. */
    @Transactional
    public void deleteScan(Long id) {
        Scan scan = requireScan(id);
        copilotMessageRepository.deleteByScanId(id);
        findingRepository.deleteByScanId(id);
        scoreRepository.deleteByScanId(id);
        scanRepository.delete(scan);
    }

    /** Recomputes the human readable scoring notes from the stored rows. */
    private List<String> scoringNotes(List<FindingResponse> findings, List<CategoryScoreResponse> scores) {
        List<String> notes = new ArrayList<>();
        for (CategoryScoreResponse score : scores) {
            if (score.score() == null) {
                notes.add(score.label() + ": not enough data - only " + score.checksApplicable() + " of "
                        + score.checksDefined() + " checks could be evaluated.");
            } else {
                notes.add(score.label() + ": " + score.checksPassed() + " of " + score.checksApplicable()
                        + " applicable checks passed = " + score.score() + "/100.");
            }
        }
        if (findings.stream().noneMatch(f -> f.status() == CheckStatus.NOT_RUN)) {
            notes.add("All checks that could run for this page were executed.");
        } else {
            notes.add("Some checks could not be executed and were excluded from the denominator.");
        }
        return notes;
    }

    private static String hostOf(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? url : uri.getHost();
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    /** Error messages are shown to users - keep them short and single-line. */
    private static String safeError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "The scan failed: " + e.getClass().getSimpleName();
        }
        return truncate(message.replaceAll("[\\r\\n]+", " ").trim(), 1000);
    }

    /**
     * Scans interrupted by a restart would otherwise stay "running" forever.
     */
    @Transactional
    public void failInterruptedScans() {
        for (Scan scan : scanRepository.findAll()) {
            if (scan.getStatus().isRunning()) {
                scan.setStatus(ScanStatus.FAILED);
                scan.setStage(null);
                scan.setErrorMessage("This scan was interrupted by an application restart. Run it again.");
                scan.setCompletedAt(Instant.now());
                scanRepository.save(scan);
            }
        }
    }
}
