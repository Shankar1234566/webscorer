package com.webscorer.entity;

import com.webscorer.domain.ScanStage;
import com.webscorer.domain.ScanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One website scan. Scores stay nullable on purpose: {@code null} means
 * "not enough data", never "zero".
 */
@Entity
@Table(name = "scans", indexes = {
        @Index(name = "idx_scans_created_at", columnList = "created_at"),
        @Index(name = "idx_scans_url", columnList = "url")
})
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String url;

    /** Final URL after redirects, when it differs from the requested one. */
    @Column(length = 2048)
    private String finalUrl;

    @Column(length = 512)
    private String pageTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ScanStage stage;

    @Column(name = "overall_score")
    private Integer overallScore;

    /** Evidence-based one paragraph summary, generated from real findings. */
    @Column(length = 2000)
    private String summary;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "checks_passed")
    private Integer checksPassed;

    @Column(name = "checks_failed")
    private Integer checksFailed;

    @Column(name = "checks_not_run")
    private Integer checksNotRun;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFinalUrl() {
        return finalUrl;
    }

    public void setFinalUrl(String finalUrl) {
        this.finalUrl = finalUrl;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public void setStatus(ScanStatus status) {
        this.status = status;
    }

    public ScanStage getStage() {
        return stage;
    }

    public void setStage(ScanStage stage) {
        this.stage = stage;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getChecksPassed() {
        return checksPassed;
    }

    public void setChecksPassed(Integer checksPassed) {
        this.checksPassed = checksPassed;
    }

    public Integer getChecksFailed() {
        return checksFailed;
    }

    public void setChecksFailed(Integer checksFailed) {
        this.checksFailed = checksFailed;
    }

    public Integer getChecksNotRun() {
        return checksNotRun;
    }

    public void setChecksNotRun(Integer checksNotRun) {
        this.checksNotRun = checksNotRun;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
