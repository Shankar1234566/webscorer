package com.webscorer.service;

import com.webscorer.analysis.CheckResult;
import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Result of {@link ScoringService#score(List)}: per category scores plus the
 * overall roll-up. Every number is traceable to stored check results.
 *
 * @param categoryScores score per category, null score means "not enough data"
 * @param overallScore   null means "not enough data"
 * @param checksPassed   number of checks with status PASS
 * @param checksFailed   number of checks with status FAIL
 * @param checksNotRun   number of checks with status NOT_RUN
 * @param notes          human readable scoring decisions for the UI
 */
public record ScoreOutcome(
        Map<Category, CategoryScoreValue> categoryScores,
        Integer overallScore,
        int checksPassed,
        int checksFailed,
        int checksNotRun,
        List<String> notes
) {
    public record CategoryScoreValue(Integer score, int passed, int applicable, int notRun, int defined) {
    }

    public static ScoreOutcome empty() {
        return new ScoreOutcome(Collections.emptyMap(), null, 0, 0, 0, List.of());
    }
}
