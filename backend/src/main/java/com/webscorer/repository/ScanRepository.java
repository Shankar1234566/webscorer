package com.webscorer.repository;

import com.webscorer.entity.Scan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanRepository extends JpaRepository<Scan, Long> {

    Optional<Scan> findFirstByUrlOrderByCreatedAtDesc(String url);

    List<Scan> findAllByOrderByCreatedAtDesc();

    List<Scan> findTop2ByUrlOrderByCreatedAtDesc(String url);
}
