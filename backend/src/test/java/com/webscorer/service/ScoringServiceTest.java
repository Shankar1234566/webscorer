package com.webscorer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.webscorer.analysis.CheckCatalog;
import com.webscorer.analysis.CheckResult;
import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import com.webscorer.domain.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScoringServiceTest {

    private final ScoringService service = new ScoringService();

    private CheckResult result(CheckDefinitionStub stub, CheckStatus status) {
        return new CheckResult(stub.id(), stub.category(), stub.severity(), status, stub.title(),
                "evidence", "why", "fix", null, 1);
    }

    /** Tiny helper so the test data reads like the real catalog. */
    private record CheckDefinitionStub(String id, Category category, Severity severity, String title) {
    }

    private List<CheckResult> seoScenario(int passed, int failed, int notRun) {
        List<CheckResult> results = new java.util.ArrayList<>();
        for (int i = 0; i < passed; i++) {
            results.add(result(new CheckDefinitionStub("seo.p" + i, Category.SEO, Severity.LOW, "pass " + i), CheckStatus.PASS));
        }
        for (int i = 0; i < failed; i++) {
            results.add(result(new CheckDefinitionStub("seo.f" + i, Category.SEO, Severity.HIGH, "fail " + i), CheckStatus.FAIL));
        }
        for (int i = 0; i < notRun; i++) {
            results.add(result(new CheckDefinitionStub("seo.n" + i, Category.SEO, Severity.LOW, "notrun " + i), CheckStatus.NOT_RUN));
        }
        return results;
    }

    @Test
    void categoryScoreIsPassedOverApplicable() {
        // 4 pass + 2 fail = 6 applicable -> 4/6 = 67%
        ScoreOutcome outcome = service.score(seoScenario(4, 2, 0));
        ScoreOutcome.CategoryScoreValue seo = outcome.categoryScores().get(Category.SEO);
        assertThat(seo.score()).isEqualTo(67);
        assertThat(seo.applicable()).isEqualTo(6);
        assertThat(seo.passed()).isEqualTo(4);
    }

    @Test
    void notRunChecksNeverLowerAScore() {
        // 5 pass + 3 not run: the missing evidence is excluded, score stays 100.
        // (SEO defines 8 checks, so 5 evaluated checks clear the 60% threshold.)
        ScoreOutcome outcome = service.score(seoScenario(5, 0, 3));
        assertThat(outcome.categoryScores().get(Category.SEO).score()).isEqualTo(100);
        assertThat(outcome.categoryScores().get(Category.SEO).notRun()).isEqualTo(3);
        assertThat(outcome.checksNotRun()).isEqualTo(3);
    }

    @Test
    void notApplicableChecksAreExcludedFromTheDenominator() {
        // Accessibility defines 6 checks: 4 that apply and all pass, 2 that do
        // not apply to this page. Score = 4/4 = 100, not 4/6.
        List<CheckResult> results = List.of(
                new CheckResult("a", Category.ACCESSIBILITY, Severity.INFO, CheckStatus.PASS, "pass", "e", "w", "f", null, 1),
                new CheckResult("b", Category.ACCESSIBILITY, Severity.INFO, CheckStatus.PASS, "pass", "e", "w", "f", null, 1),
                new CheckResult("c", Category.ACCESSIBILITY, Severity.INFO, CheckStatus.PASS, "pass", "e", "w", "f", null, 1),
                new CheckResult("d", Category.ACCESSIBILITY, Severity.INFO, CheckStatus.PASS, "pass", "e", "w", "f", null, 1),
                new CheckResult("e", Category.ACCESSIBILITY, Severity.INFO, CheckStatus.NOT_APPLICABLE, "na", "e", "w", "f", null, 1),
                new CheckResult("f", Category.ACCESSIBILITY, Severity.INFO, CheckStatus.NOT_APPLICABLE, "na", "e", "w", "f", null, 1));
        ScoreOutcome outcome = service.score(results);
        assertThat(outcome.categoryScores().get(Category.ACCESSIBILITY).score()).isEqualTo(100);
        assertThat(outcome.categoryScores().get(Category.ACCESSIBILITY).applicable()).isEqualTo(4);
    }

    @Test
    void categoryWithTooFewEvaluatedChecksReportsNotEnoughData() {
        // Only 2 of 8 SEO checks could run - below the 60% threshold.
        ScoreOutcome outcome = service.score(seoScenario(2, 0, 6));
        assertThat(outcome.categoryScores().get(Category.SEO).score()).isNull();
        assertThat(outcome.notes()).isNotEmpty();
    }

    @Test
    void overallScoreIsAWeightedMeanOfQualifiedCategories() {
        // Qualify every category with full marks, then drop SEO to 0 to prove weighting.
        List<CheckResult> results = new java.util.ArrayList<>();
        addAll(results, Category.SEO, 0, 8);
        addAll(results, Category.ACCESSIBILITY, 6, 6);
        addAll(results, Category.TECHNICAL, 7, 7);
        addAll(results, Category.CONTENT, 4, 4);

        ScoreOutcome outcome = service.score(results);
        // (0*0.35 + 100*0.25 + 100*0.25 + 100*0.15) / 1.00 = 65
        assertThat(outcome.categoryScores().get(Category.SEO).score()).isEqualTo(0);
        assertThat(outcome.overallScore()).isEqualTo(65);
    }

    @Test
    void overallScoreIsNullWhenFewerThanTwoCategoriesQualify() {
        List<CheckResult> results = new java.util.ArrayList<>();
        addAll(results, Category.SEO, 8, 8);
        addAll(results, Category.ACCESSIBILITY, 0, 6);
        addAll(results, Category.TECHNICAL, 0, 7);
        addAll(results, Category.CONTENT, 0, 4);
        // Everything except SEO is NOT_RUN, so only one category qualifies.
        results.removeIf(r -> r.category() != Category.SEO && r.status() != CheckStatus.NOT_RUN);

        ScoreOutcome outcome = service.score(results);
        assertThat(outcome.categoryScores().get(Category.SEO).score()).isEqualTo(100);
        assertThat(outcome.overallScore()).isNull();
    }

    @Test
    void scoringIsDeterministicForTheSameResults() {
        List<CheckResult> results = seoScenario(5, 3, 1);
        ScoreOutcome first = service.score(results);
        ScoreOutcome second = service.score(results);
        assertThat(first.overallScore()).isEqualTo(second.overallScore());
        assertThat(first.categoryScores()).isEqualTo(second.categoryScores());
    }

    @Test
    void catalogMatchesTheDocumentedCheckCount() {
        assertThat(CheckCatalog.ALL).hasSize(25);
        assertThat(service.definedCheckCount()).isEqualTo(25);
        assertThat(ScoringService.CATEGORY_WEIGHTS.values().stream().mapToDouble(Double::doubleValue).sum())
                .isEqualTo(1.0d).as("category weights must add up to 1");
    }

    private void addAll(List<CheckResult> results, Category category, int passed, int total) {
        for (int i = 0; i < total; i++) {
            results.add(new CheckResult(category.name().toLowerCase() + "." + i, category, Severity.LOW,
                    i < passed ? CheckStatus.PASS : CheckStatus.FAIL, "check " + i, "evidence", "why", "fix", null, 1));
        }
    }
}
