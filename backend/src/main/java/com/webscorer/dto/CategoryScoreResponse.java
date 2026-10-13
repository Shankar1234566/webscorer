package com.webscorer.dto;

import com.webscorer.domain.Category;
import com.webscorer.entity.CategoryScore;

/** Score of one category; {@code score} is null when there is not enough data. */
public record CategoryScoreResponse(
        Category category,
        String label,
        String hint,
        Integer score,
        int checksPassed,
        int checksApplicable,
        int checksNotRun,
        int checksDefined
) {
    public static CategoryScoreResponse from(CategoryScore score, int defined) {
        return new CategoryScoreResponse(
                score.getCategory(),
                score.getCategory().getLabel(),
                score.getCategory().getHint(),
                score.getScore(),
                score.getChecksPassed(),
                score.getChecksApplicable(),
                score.getChecksNotRun(),
                defined);
    }
}
