package com.webscorer.dto;

import java.util.List;

/** Full report payload returned by GET /api/scans/{id}. */
public record ScanDetailResponse(
        ScanSummaryResponse scan,
        List<CategoryScoreResponse> categoryScores,
        List<FindingResponse> findings,
        List<String> scoringNotes,
        Long previousScanId,
        Integer previousScore,
        java.time.Instant previousScanAt,
        boolean sample
) {
}
