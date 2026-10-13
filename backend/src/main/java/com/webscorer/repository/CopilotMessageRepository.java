package com.webscorer.repository;

import com.webscorer.entity.CopilotMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CopilotMessageRepository extends JpaRepository<CopilotMessage, Long> {

    List<CopilotMessage> findByScanIdOrderByIdAsc(Long scanId);

    @Transactional
    void deleteByScanId(Long scanId);
}
