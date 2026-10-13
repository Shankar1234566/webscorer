package com.webscorer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.webscorer.config.AiProperties;
import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import com.webscorer.domain.Severity;
import com.webscorer.dto.CategoryScoreResponse;
import com.webscorer.dto.FindingResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The fallback copilot must answer from report data only and stay honest about
 * what the scan did and did not measure.
 */
class LocalCopilotServiceTest {

    private LocalCopilotService service;
    private CopilotContext context;

    @BeforeEach
    void setUp() {
        service = new LocalCopilotService();
        FindingResponse topIssue = new FindingResponse(1L, "tech.https", Category.TECHNICAL, Severity.CRITICAL,
                CheckStatus.FAIL, "Site is served over HTTPS",
                "The page was served over plain HTTP (http://example.org/).",
                "Plain HTTP lets anyone on the network read or modify the page in transit.",
                "Serve the site over HTTPS and redirect every HTTP request to HTTPS.",
                "server {\n  listen 80;\n  return 301 https://$host$request_uri;\n}", 1);
        FindingResponse pass = new FindingResponse(2L, "a11y.lang", Category.ACCESSIBILITY, Severity.INFO,
                CheckStatus.PASS, "Document declares its language", "<html lang=\"en\"> declared.",
                "The lang attribute decides which pronunciation a screen reader uses.", null, null, 1);

        context = new CopilotContext(
                "http://example.org/",
                64,
                "Fetched example.org with HTTP 200 in 210 ms. 18 of 25 applicable checks passed.",
                List.of(new CategoryScoreResponse(Category.TECHNICAL, "Technical", "Technical HTML checks", 86, 6, 7, 0, 7)),
                List.of(topIssue, pass),
                Map.of("2026-10-01", 58));
    }

    @Test
    void noKeyConfiguredMeansLocalProvider() {
        assertThat(service.providerName()).isEqualTo("LOCAL");
        AiProperties properties = new AiProperties();
        assertThat(properties.isEnabled()).as("a fresh AiProperties must not claim an API key").isFalse();
    }

    @Test
    void prioritisationQuestionReturnsTheHighestSeverityIssueWithEvidence() {
        CopilotAnswer answer = service.answer(context, "What should I fix first?");
        assertThat(answer.provider()).isEqualTo("LOCAL");
        assertThat(answer.message()).contains("Site is served over HTTPS");
        assertThat(answer.message()).contains("The page was served over plain HTTP");
        assertThat(answer.message()).contains("CRITICAL");
        assertThat(answer.note()).contains("no AI key is configured");
    }

    @Test
    void scoreQuestionExplainsTheCalculation() {
        CopilotAnswer answer = service.answer(context, "Explain my website score");
        assertThat(answer.message()).contains("64/100");
        assertThat(answer.message()).contains("weighted mean");
        assertThat(answer.message()).contains("SEO 35%");
        assertThat(answer.message()).contains("2026-10-01");
    }

    @Test
    void rankingQuestionsAreRefusedHonestly() {
        CopilotAnswer answer = service.answer(context, "Can you guarantee I rank #1 on Google?");
        assertThat(answer.message()).containsIgnoringCase("can't promise");
        assertThat(answer.message()).doesNotContain("guaranteed");
    }

    @Test
    void performanceQuestionsAdmitItWasNotMeasured() {
        CopilotAnswer answer = service.answer(context, "What is my page speed?");
        assertThat(answer.message()).containsIgnoringCase("did not measure performance");
        assertThat(answer.message()).doesNotContain("score of");
    }

    @Test
    void codeQuestionReturnsTheExampleAndLabelsItAsAnExample() {
        CopilotAnswer answer = service.answer(context, "Generate an example code fix");
        assertThat(answer.message()).contains("listen 80");
        assertThat(answer.message()).contains("not applied anything");
    }

    @Test
    void securityQuestionsStayWithinScope() {
        CopilotAnswer answer = service.answer(context, "Is my site secure? do you run a penetration test?");
        assertThat(answer.message()).containsIgnoringCase("not a security audit");
    }

    @Test
    void unknownQuestionFallsBackToTheReportSnapshot() {
        CopilotAnswer answer = service.answer(context, "tell me something random about penguins");
        assertThat(answer.message()).contains("Report snapshot");
        assertThat(answer.message()).contains("example.org");
    }

    @Test
    void answersNeverClaimAFixWasApplied() {
        for (String question : List.of("What should I fix first?", "How can I fix this issue?",
                "Generate an example code fix", "Explain the SEO findings", "Explain my website score")) {
            CopilotAnswer answer = service.answer(context, question);
            assertThat(answer.message().toLowerCase()).doesNotContain("i have applied");
            assertThat(answer.message().toLowerCase()).doesNotContain("already fixed");
        }
    }

    @Test
    void issuesAreSortedBySeverityFirstThenCategoryWeight() {
        FindingResponse lowContent = finding("content.headings", Category.CONTENT, Severity.LOW,
                "Headings divide the content");
        FindingResponse highSeo = finding("seo.description.present", Category.SEO, Severity.HIGH,
                "Meta description exists");
        FindingResponse mediumContent = finding("content.text", Category.CONTENT, Severity.MEDIUM,
                "Page has meaningful text content");
        FindingResponse highTechnical = finding("tech.https", Category.TECHNICAL, Severity.HIGH,
                "Site is served over HTTPS");
        FindingResponse lowSeo = finding("seo.canonical.present", Category.SEO, Severity.LOW,
                "Canonical URL is declared");

        CopilotContext mixed = new CopilotContext("http://example.org/", 50,
                "Mixed report.",
                List.of(),
                List.of(lowContent, highSeo, mediumContent, highTechnical, lowSeo),
                Map.of());

        List<FindingResponse> sorted = service.sortedIssues(mixed);

        assertThat(sorted).extracting(FindingResponse::title)
                .containsExactly("Meta description exists", "Site is served over HTTPS",
                        "Page has meaningful text content", "Canonical URL is declared",
                        "Headings divide the content");
    }

    private FindingResponse finding(String checkId, Category category, Severity severity, String title) {
        return new FindingResponse(9L, checkId, category, severity, CheckStatus.FAIL, title,
                "Evidence for " + title, "Why it matters.", "How to fix it.", null, 1);
    }
}
