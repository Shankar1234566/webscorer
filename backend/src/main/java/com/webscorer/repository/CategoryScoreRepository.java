package com.webscorer.repository;

import com.webscorer.entity.CategoryScore;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CategoryScoreRepository extends JpaRepository<CategoryScore, Long> {

    List<CategoryScore> findByScanId(Long scanId);

    @Transactional
    void deleteByScanId(Long scanId);
}
