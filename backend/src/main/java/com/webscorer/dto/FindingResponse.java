package com.webscorer.dto;

import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import com.webscorer.domain.Severity;
import com.webscorer.entity.ScanFinding;

/** A single check result as exposed by the REST API. */
public record FindingResponse(
        Long id,
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
    public static FindingResponse from(ScanFinding finding) {
        return new FindingResponse(
                finding.getId(),
                finding.getCheckId(),
                finding.getCategory(),
                finding.getSeverity(),
                finding.getStatus(),
                finding.getTitle(),
                finding.getEvidence(),
                finding.getExplanation(),
                finding.getRecommendation(),
                finding.getCodeExample(),
                finding.getWeight());
    }
}
