package com.webscorer.domain;

/**
 * Outcome of a single check.
 *
 * <p>{@code NOT_RUN} is deliberately different from {@code FAIL}: a check that
 * could not execute (for example the document never downloaded) is missing
 * evidence, not a proven problem, and it must never lower a score.</p>
 */
public enum CheckStatus {
    /** The check ran and the page satisfies it. */
    PASS,
    /** The check ran and the page violates it. */
    FAIL,
    /** The check ran but does not apply to this page (e.g. no iframes present). */
    NOT_APPLICABLE,
    /** The check could not be executed, so there is no evidence either way. */
    NOT_RUN
}
