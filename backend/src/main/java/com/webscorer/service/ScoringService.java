package com.webscorer.service;

import com.webscorer.analysis.CheckCatalog;
import com.webscorer.analysis.CheckDefinition;
import com.webscorer.analysis.CheckResult;
import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Deterministic scoring engine.
 *
 * <p>Rules (mirrored in docs/scoring-rules.md):</p>
 * <ol>
 *   <li>Only PASS and FAIL checks enter the denominator. NOT_APPLICABLE checks
 *       do not exist for this page; NOT_RUN checks have no evidence and must
 *       never lower a score.</li>
 *   <li>category score = round(100 x passed weight / applicable weight).</li>
 *   <li>A category reports a score only if at least 60% of its defined checks
 *       could be evaluated (minimum 2), otherwise it reports "not enough data".</li>
 *   <li>The overall score is the weighted mean of the categories that have a
 *       score: SEO 35%, Accessibility 25%, Technical 25%, Content 15%. It is
 *       reported only when at least two categories qualify and their combined
 *       weight reaches half of the full weight.</li>
 * </ol>
 *
 * <p>No LLM is involved anywhere in this class.</p>
 */
@Service
public class ScoringService {

    public static final Map<Category, Double> CATEGORY_WEIGHTS = Map.of(
            Category.SEO, 0.35d,
            Category.ACCESSIBILITY, 0.25d,
            Category.TECHNICAL, 0.25d,
            Category.CONTENT, 0.15d);

    public ScoreOutcome score(List<CheckResult> results) {
        Map<Category, Integer> passedWeight = new EnumMap<>(Category.class);
        Map<Category, Integer> applicableWeight = new EnumMap<>(Category.class);
        Map<Category, Integer> passedCount = new EnumMap<>(Category.class);
        Map<Category, Integer> notRunCount = new EnumMap<>(Category.class);

        int totalPassed = 0;
        int totalFailed = 0;
        int totalNotRun = 0;

        for (CheckResult result : results) {
            CheckStatus status = result.status();
            if (status == CheckStatus.NOT_RUN) {
                notRunCount.merge(result.category(), 1, Integer::sum);
                totalNotRun++;
                continue;
            }
            if (status == CheckStatus.NOT_APPLICABLE) {
                continue;
            }
            applicableWeight.merge(result.category(), result.weight(), Integer::sum);
            if (status == CheckStatus.PASS) {
                passedWeight.merge(result.category(), result.weight(), Integer::sum);
                passedCount.merge(result.category(), 1, Integer::sum);
                totalPassed++;
            } else {
                totalFailed++;
            }
        }

        Map<Category, ScoreOutcome.CategoryScoreValue> categoryScores = new EnumMap<>(Category.class);
        List<String> notes = new ArrayList<>();
        double weightedTotal = 0.0d;
        double usedWeight = 0.0d;
        int scoredCategories = 0;

        for (Category category : Category.values()) {
            int defined = CheckCatalog.definedCount(category);
            int applicable = applicableWeight.getOrDefault(category, 0);
            int notRun = notRunCount.getOrDefault(category, 0);
            int passed = passedCount.getOrDefault(category, 0);

            int threshold = Math.max(2, (int) Math.ceil(defined * 0.6d));
            if (applicable < threshold) {
                categoryScores.put(category, new ScoreOutcome.CategoryScoreValue(null, passed, applicable, notRun, defined));
                notes.add(category.getLabel() + ": not enough data - only " + applicable + " of " + defined
                        + " checks could be evaluated (need at least " + threshold + ").");
                continue;
            }

            int weight = applicableWeight.get(category);
            int score = (int) Math.round(100.0d * passedWeight.getOrDefault(category, 0) / weight);
            categoryScores.put(category, new ScoreOutcome.CategoryScoreValue(score, passed, applicable, notRun, defined));
            notes.add(category.getLabel() + ": " + passed + " of " + applicable + " applicable checks passed = "
                    + score + "/100.");
            weightedTotal += score * CATEGORY_WEIGHTS.get(category);
            usedWeight += CATEGORY_WEIGHTS.get(category);
            scoredCategories++;
        }

        Integer overall = null;
        double minimumWeight = 0.5d;
        if (scoredCategories >= 2 && usedWeight >= minimumWeight) {
            overall = (int) Math.round(weightedTotal / usedWeight);
            notes.add("Overall score = weighted mean over " + scoredCategories
                    + " categories (combined weight " + String.format("%.2f", usedWeight) + ").");
        } else {
            notes.add("Overall score withheld: fewer than two categories had enough evidence.");
        }

        return new ScoreOutcome(categoryScores, overall, totalPassed, totalFailed, totalNotRun, notes);
    }

    /** Convenience for tests: how many definitions exist in total. */
    public int definedCheckCount() {
        return CheckCatalog.ALL.size();
    }

    public static int definedCheckCount(Category category) {
        return CheckCatalog.definedCount(category);
    }
}
