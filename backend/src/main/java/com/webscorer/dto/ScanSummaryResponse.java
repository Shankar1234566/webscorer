package com.webscorer.dto;

import com.webscorer.domain.ScanStage;
import com.webscorer.domain.ScanStatus;
import com.webscorer.entity.Scan;
import java.time.Instant;

/** Headline scan information; also the polling payload while a scan runs. */
public record ScanSummaryResponse(
        Long id,
        String url,
        String finalUrl,
        String pageTitle,
        ScanStatus status,
        ScanStage stage,
        Integer overallScore,
        String summary,
        Integer httpStatus,
        Long durationMs,
        Integer checksPassed,
        Integer checksFailed,
        Integer checksNotRun,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {
    public static ScanSummaryResponse from(Scan scan) {
        return new ScanSummaryResponse(
                scan.getId(),
                scan.getUrl(),
                scan.getFinalUrl(),
                scan.getPageTitle(),
                scan.getStatus(),
                scan.getStage(),
                scan.getOverallScore(),
                scan.getSummary(),
                scan.getHttpStatus(),
                scan.getDurationMs(),
                scan.getChecksPassed(),
                scan.getChecksFailed(),
                scan.getChecksNotRun(),
                scan.getErrorMessage(),
                scan.getCreatedAt(),
                scan.getCompletedAt());
    }
}
