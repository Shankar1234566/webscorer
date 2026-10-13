package com.webscorer.analysis;

import com.webscorer.domain.Category;
import com.webscorer.domain.Severity;

/**
 * Static definition of one check: what it measures and what a failure means.
 * The catalog is the single source of truth for both scoring and the docs.
 *
 * @param id           stable identifier, e.g. "seo.title.present"
 * @param category     scored group
 * @param severity     severity used when the check fails
 * @param weight       contribution to the category score (all checks are 1 today)
 * @param title        human readable check name
 * @param whyItMatters explanation shown on failing findings
 * @param howToFix     recommended action shown on failing findings
 * @param codeExample  optional illustrative snippet, never applied automatically
 */
public record CheckDefinition(
        String id,
        Category category,
        Severity severity,
        int weight,
        String title,
        String whyItMatters,
        String howToFix,
        String codeExample
) {
    public CheckDefinition {
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive for " + id);
        }
    }
}
