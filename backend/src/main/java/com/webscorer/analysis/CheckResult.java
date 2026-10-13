package com.webscorer.analysis;

import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import com.webscorer.domain.Severity;

/**
 * Outcome of one executed check. Mirrors the {@code scan_findings} row but is
 * immutable and used while the scan is still running.
 */
public record CheckResult(
        String checkId,
        Category category,
        Severity severity,
        CheckStatus status,
        String title,
        String evidence,
        String explanation,
        String recommendation,
        String codeExample,
        int weight
) {
    public boolean countsTowardsScore() {
        return status == CheckStatus.PASS || status == CheckStatus.FAIL;
    }

    public boolean isIssue() {
        return status == CheckStatus.FAIL;
    }
}
