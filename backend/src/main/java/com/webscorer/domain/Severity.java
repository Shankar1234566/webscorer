package com.webscorer.domain;

/** How much a failed check hurts the report. */
public enum Severity {
    CRITICAL(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1),
    INFO(0);

    private final int rank;

    Severity(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}
