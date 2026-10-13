package com.webscorer.repository;

import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import com.webscorer.entity.ScanFinding;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ScanFindingRepository extends JpaRepository<ScanFinding, Long> {

    List<ScanFinding> findByScanIdOrderByCategoryAscIdAsc(Long scanId);

    List<ScanFinding> findByScanIdAndStatus(Long scanId, CheckStatus status);

    long countByScanIdAndStatus(Long scanId, CheckStatus status);

    long countByScanIdAndCategoryAndStatus(Long scanId, Category category, CheckStatus status);

    @Transactional
    void deleteByScanId(Long scanId);
}
