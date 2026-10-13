package com.webscorer.entity;

import com.webscorer.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Deterministic score for one category of a scan.
 * {@code score} is null when too few checks could be evaluated.
 */
@Entity
@Table(name = "category_scores", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(name = "uq_score_scan_category", columnNames = {"scan_id", "category"})
}, indexes = {
        @Index(name = "idx_scores_scan", columnList = "scan_id")
})
public class CategoryScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scan_id", nullable = false)
    private Long scanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(name = "score")
    private Integer score;

    /** Checks that returned PASS. */
    @Column(name = "checks_passed")
    private int checksPassed;

    /** Checks that actually ran and can count towards the denominator. */
    @Column(name = "checks_applicable")
    private int checksApplicable;

    /** Checks that could not be executed - missing evidence, not failures. */
    @Column(name = "checks_not_run")
    private int checksNotRun;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScanId() {
        return scanId;
    }

    public void setScanId(Long scanId) {
        this.scanId = scanId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public int getChecksPassed() {
        return checksPassed;
    }

    public void setChecksPassed(int checksPassed) {
        this.checksPassed = checksPassed;
    }

    public int getChecksApplicable() {
        return checksApplicable;
    }

    public void setChecksApplicable(int checksApplicable) {
        this.checksApplicable = checksApplicable;
    }

    public int getChecksNotRun() {
        return checksNotRun;
    }

    public void setChecksNotRun(int checksNotRun) {
        this.checksNotRun = checksNotRun;
    }
}
