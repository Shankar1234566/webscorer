package com.webscorer.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import com.webscorer.service.ScoringService;
import com.webscorer.service.ScoreOutcome;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

/**
 * Runs the analyzers against a small fixture document - no network involved.
 */
class HtmlAnalyzerTest {

    private final HtmlAnalyzer analyzer = new HtmlAnalyzer();

    private Map<String, CheckResult> analyze(String html, String url) {
        Document doc = Jsoup.parse(html, url);
        PageContext ctx = new PageContext(URI.create(url), doc, url, 200, 120, html.length());
        return analyzer.analyze(ctx).stream()
                .collect(Collectors.toMap(CheckResult::checkId, Function.identity()));
    }

    @Test
    void aWellFormedPagePassesTheCoreChecks() {
        String html = """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Website quality scanner for teams</title>
                    <meta name="description" content="Scan any public website for SEO, accessibility and technical issues.">
                    <link rel="canonical" href="https://example.com/">
                  </head>
                  <body>
                    <nav aria-label="Primary"><a href="/pricing">Pricing</a><a href="/docs">Docs</a></nav>
                    <h1>Website quality scanner</h1>
                    <h2>What you get</h2>
                    <p>%s</p>
                    <img src="/team.png" alt="The four members of the design team">
                    <label for="email">Email</label><input id="email" type="email" name="email">
                    <a href="/reports">Read the full audit report</a>
                    <button type="submit">Run scan</button>
                  </body>
                </html>
                """.formatted(wordyParagraph());

        Map<String, CheckResult> results = analyze(html, "https://example.com/");

        assertThat(results.get("seo.title.present").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("seo.title.length").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("seo.description.present").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("seo.h1.present").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("seo.canonical.present").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("a11y.lang").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("a11y.images.alt").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("a11y.form.labels").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("tech.https").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("tech.viewport").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("content.text").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("content.navigation").status()).isEqualTo(CheckStatus.PASS);
        // no iframes on the page -> not applicable, not a failure
        assertThat(results.get("a11y.iframe.title").status()).isEqualTo(CheckStatus.NOT_APPLICABLE);
        assertThat(results).hasSize(CheckCatalog.ALL.size());

        // every result must reference a known check id
        assertThat(results.keySet()).containsExactlyInAnyOrderElementsOf(
                CheckCatalog.ALL.stream().map(CheckDefinition::id).toList());
    }

    @Test
    void httpPagesAndMissingMetadataFailTheExpectedChecks() {
        String html = """
                <html>
                  <head><title>Hi</title></head>
                  <body>
                    <h1>Only a heading</h1>
                    <p>Short.</p>
                    <img src="/a.png">
                    <a href="">empty target</a>
                    <a href="#">hash only</a>
                    <a href="javascript:void(0)">js</a>
                    <a>no href</a>
                  </body>
                </html>
                """;
        Map<String, CheckResult> results = analyze(html, "http://example.org/");

        assertThat(results.get("tech.https").status()).isEqualTo(CheckStatus.FAIL);
        assertThat(results.get("tech.https").severity()).isEqualTo(com.webscorer.domain.Severity.CRITICAL);
        assertThat(results.get("seo.title.present").status()).isEqualTo(CheckStatus.PASS);
        assertThat(results.get("seo.title.length").status()).isEqualTo(CheckStatus.FAIL);
        assertThat(results.get("seo.description.present").status()).isEqualTo(CheckStatus.FAIL);
        assertThat(results.get("a11y.lang").status()).isEqualTo(CheckStatus.FAIL);
        assertThat(results.get("a11y.images.alt").status()).isEqualTo(CheckStatus.FAIL);
        assertThat(results.get("content.text").status()).isEqualTo(CheckStatus.FAIL);
        assertThat(results.get("tech.links.targets").status()).isEqualTo(CheckStatus.FAIL);
        assertThat(results.get("tech.mixed.content").status()).isEqualTo(CheckStatus.NOT_APPLICABLE);
        assertThat(results.get("content.link.text").status()).isEqualTo(CheckStatus.PASS);
    }

    @Test
    void noindexMetaBlocksIndexabilityCheck() {
        String html = """
                <html><head><meta name="robots" content="noindex, follow">
                <title>A perfectly reasonable page title</title></head>
                <body><p>hello</p></body></html>
                """;
        Map<String, CheckResult> results = analyze(html, "https://example.com/draft");
        assertThat(results.get("seo.robots.indexable").status()).isEqualTo(CheckStatus.FAIL);
        assertThat(results.get("seo.robots.indexable").evidence()).contains("noindex");
    }

    @Test
    void indexableFailureCarriesHighSeverityAndAnAccurateExplanation() {
        String html = """
                <html><head><meta name="robots" content="noindex">
                <title>Draft of a page under review</title></head>
                <body><p>hello</p></body></html>
                """;
        CheckResult result = analyze(html, "https://example.com/draft").get("seo.robots.indexable");

        assertThat(result.status()).isEqualTo(CheckStatus.FAIL);
        // A noindex hides the page from search entirely - HIGH matches the impact.
        assertThat(result.severity()).isEqualTo(com.webscorer.domain.Severity.HIGH);
        assertThat(result.evidence()).contains("<meta name=\"robots\" content=\"noindex\">").contains("blocks indexing");
        assertThat(result.explanation()).contains("removes the page from search results entirely");
        assertThat(result.recommendation()).contains("noindex");
    }

    @Test
    void absentRobotsTagPassesWithHonestEvidence() {
        String html = """
                <html lang="en"><head><title>A page with no robots tag at all</title></head>
                <body><p>hello there</p></body></html>
                """;
        CheckResult result = analyze(html, "https://example.com/").get("seo.robots.indexable");

        assertThat(result.status()).isEqualTo(CheckStatus.PASS);
        assertThat(result.evidence()).isEqualTo("No meta robots tag - the default (index, follow) applies.");
    }

    @Test
    void emptyRobotsTagDoesNotClaimTheTagIsMissing() {
        String html = """
                <html lang="en"><head><title>A page with an empty robots tag</title>
                <meta name="robots" content="">
                </head><body><p>hello there</p></body></html>
                """;
        CheckResult result = analyze(html, "https://example.com/").get("seo.robots.indexable");

        assertThat(result.status()).isEqualTo(CheckStatus.PASS);
        // Evidence must not say "No meta robots tag" when one exists.
        assertThat(result.evidence()).doesNotContain("No meta robots tag");
        assertThat(result.evidence()).contains("carries no content");
    }

    @Test
    void crawlerSpecificNoindexIsDetectedCaseInsensitively() {
        String html = """
                <html><head><meta name="GoogleBot" content="NOINDEX">
                <title>Google-only exclusions count too</title></head>
                <body><p>hello</p></body></html>
                """;
        CheckResult result = analyze(html, "https://example.com/news").get("seo.robots.indexable");

        assertThat(result.status()).isEqualTo(CheckStatus.FAIL);
        // Evidence quotes the tag as it appears in the page.
        assertThat(result.evidence()).contains("GoogleBot").contains("blocks indexing");
    }

    @Test
    void explicitIndexDirectivePasses() {
        String html = """
                <html><head><meta name="robots" content="index, follow">
                <title>A page that wants to be indexed</title></head>
                <body><p>hello</p></body></html>
                """;
        CheckResult result = analyze(html, "https://example.com/").get("seo.robots.indexable");

        assertThat(result.status()).isEqualTo(CheckStatus.PASS);
        assertThat(result.evidence()).contains("allows indexing");
    }

    @Test
    void missingDocumentProducesNotRunInsteadOfFailures() {
        List<CheckResult> results = analyzer.analyze(new PageContext(URI.create("https://x.com"), null, "https://x.com", 0, 0, 0));
        assertThat(results).hasSize(CheckCatalog.ALL.size());
        assertThat(results).allSatisfy(result -> assertThat(result.status()).isEqualTo(CheckStatus.NOT_RUN));
    }

    @Test
    void analysisIsDeterministic() {
        String html = "<html><head><title>Two word title</title></head><body><h1>Hi</h1></body></html>";
        Map<String, CheckResult> first = analyze(html, "https://example.com/a");
        Map<String, CheckResult> second = analyze(html, "https://example.com/a");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void analyzerOutputFeedsTheScoringEngine() {
        String html = """
                <html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width">
                <title>A reasonable page title for testing</title></head>
                <body><h1>Test</h1><p>%s</p></body></html>
                """.formatted(wordyParagraph());
        Map<String, CheckResult> results = analyze(html, "https://example.com/");
        ScoreOutcome outcome = new ScoringService().score(results.values().stream().toList());

        assertThat(outcome.overallScore()).isNotNull();
        assertThat(outcome.overallScore()).isBetween(0, 100);
        assertThat(outcome.categoryScores().keySet()).containsExactlyInAnyOrder(Category.values());
    }

    private static String wordyParagraph() {
        return "This paragraph exists to push the fixture comfortably past the one hundred word threshold "
                + "that the content check uses. It describes a fictional product in plain language so the "
                + "analyzer has realistic text to count, including punctuation, repeated words, words words words "
                + "and a few longer compound terms such as accessibility-friendly, search-engine and "
                + "performance-observation. Nothing here is scanned over the network: the fixture is parsed "
                + "locally, which keeps the unit test fast, offline and completely reproducible on any machine. "
                + "The copy also mentions headings, labels, alt attributes, canonical links, viewport meta tags "
                + "and character encodings, because those are exactly the things the scanner looks for in a real "
                + "document. Finally, this closing sentence adds a handful more words so the word counter lands "
                + "safely above the limit even if someone later trims a clause from the beginning of the paragraph.";
    }
}
