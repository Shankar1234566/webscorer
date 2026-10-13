package com.webscorer.service;

import com.webscorer.domain.Category;
import com.webscorer.domain.Severity;
import com.webscorer.dto.CategoryScoreResponse;
import com.webscorer.dto.FindingResponse;
import java.util.List;
import java.util.Map;

/**
 * Everything the copilot is allowed to reason about for one question.
 * Deliberately contains <em>report data only</em> - no page content is sent to
 * an external provider.
 */
public record CopilotContext(
        String url,
        Integer overallScore,
        String summary,
        List<CategoryScoreResponse> categoryScores,
        List<FindingResponse> findings,
        Map<String, Integer> historyScores
) {
    public List<FindingResponse> issues() {
        return findings.stream().filter(f -> f.status() == com.webscorer.domain.CheckStatus.FAIL).toList();
    }

    public List<FindingResponse> issuesIn(Category category) {
        return issues().stream().filter(f -> f.category() == category).toList();
    }

    public String host() {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return uri.getHost() == null ? url : uri.getHost();
        } catch (IllegalArgumentException e) {
            return url;
        }
    }
}
