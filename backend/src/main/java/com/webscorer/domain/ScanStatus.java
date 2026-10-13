package com.webscorer.domain;

/**
 * Lifecycle of a scan. The running values double as the user-visible scan
 * stage, so the frontend never has to invent a fake progress percentage.
 */
public enum ScanStatus {
    /** Accepted and queued, no work started yet. */
    PENDING,
    /** Validating the URL (scheme, DNS, SSRF guards). */
    VALIDATING,
    /** Downloading the HTML document. */
    FETCHING,
    /** Running the deterministic checks. */
    ANALYZING,
    /** Calculating category and overall scores. */
    SCORING,
    /** Terminal: report is ready. */
    COMPLETED,
    /** Terminal: something went wrong, {@code errorMessage} explains what. */
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    public boolean isRunning() {
        return !isTerminal();
    }
}
