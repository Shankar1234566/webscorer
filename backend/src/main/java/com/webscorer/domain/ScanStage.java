package com.webscorer.domain;

/** Granular stage reported while a scan is running. */
public enum ScanStage {
    VALIDATING,
    FETCHING,
    ANALYZING,
    SCORING
}
