package com.webscorer.service;

import com.webscorer.domain.ScanStatus;
import com.webscorer.dto.CategoryScoreResponse;
import com.webscorer.dto.CopilotResponse;
import com.webscorer.dto.FindingResponse;
import com.webscorer.dto.MessageResponse;
import com.webscorer.dto.ScanDetailResponse;
import com.webscorer.entity.CopilotMessage;
import com.webscorer.entity.Scan;
import com.webscorer.exception.ApiException;
import com.webscorer.repository.CopilotMessageRepository;
import com.webscorer.repository.ScanRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the copilot context from a stored report, delegates to the provider
 * and keeps the chat history.
 */
@Service
public class CopilotChatService {

    private static final DateTimeFormatter DATE_STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final ScanRepository scanRepository;
    private final WebsiteScanService scanService;
    private final CopilotRouter router;
    private final CopilotMessageRepository messageRepository;

    public CopilotChatService(ScanRepository scanRepository,
                              WebsiteScanService scanService,
                              CopilotRouter router,
                              CopilotMessageRepository messageRepository) {
        this.scanRepository = scanRepository;
        this.scanService = scanService;
        this.router = router;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public CopilotResponse ask(Long scanId, String question) {
        Scan scan = scanService.requireScan(scanId);
        if (scan.getStatus() != ScanStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This report is not ready yet - wait for the scan to finish before asking Webby.");
        }

        CopilotContext context = buildContext(scan);
        CopilotAnswer answer = router.answer(context, question.trim());

        CopilotMessage userMessage = new CopilotMessage();
        userMessage.setScanId(scanId);
        userMessage.setRole(CopilotMessage.Role.USER);
        userMessage.setMessage(question.trim());
        messageRepository.save(userMessage);

        CopilotMessage assistantMessage = new CopilotMessage();
        assistantMessage.setScanId(scanId);
        assistantMessage.setRole(CopilotMessage.Role.ASSISTANT);
        assistantMessage.setMessage(answer.message());
        assistantMessage.setProvider("LLM".equals(answer.provider()) ? CopilotMessage.Provider.LLM : CopilotMessage.Provider.LOCAL);
        messageRepository.save(assistantMessage);

        return new CopilotResponse(answer.message(), answer.provider(), answer.note(),
                LocalCopilotService.defaultSuggestions(), Instant.now());
    }

    public CopilotContext buildContext(Scan scan) {
        ScanDetailResponse detail = scanService.getDetail(scan.getId());
        Map<String, Integer> historyScores = new LinkedHashMap<>();
        for (Scan candidate : scanRepository.findTop2ByUrlOrderByCreatedAtDesc(scan.getUrl())) {
            if (!candidate.getId().equals(scan.getId()) && candidate.getStatus() == ScanStatus.COMPLETED
                    && candidate.getOverallScore() != null) {
                historyScores.put(DATE_STAMP.format(candidate.getCreatedAt()), candidate.getOverallScore());
            }
        }
        List<CategoryScoreResponse> scores = detail.categoryScores();
        List<FindingResponse> findings = detail.findings();
        return new CopilotContext(scan.getUrl(), scan.getOverallScore(), scan.getSummary(),
                scores, findings, historyScores);
    }

    public List<MessageResponse> history(Long scanId) {
        scanService.requireScan(scanId);
        List<MessageResponse> messages = new ArrayList<>();
        for (CopilotMessage message : messageRepository.findByScanIdOrderByIdAsc(scanId)) {
            messages.add(new MessageResponse(
                    message.getId(),
                    message.getRole().name(),
                    message.getMessage(),
                    message.getProvider() == null ? null : message.getProvider().name(),
                    message.getCreatedAt()));
        }
        return messages;
    }

    @Transactional
    public void clear(Long scanId) {
        scanService.requireScan(scanId);
        messageRepository.deleteByScanId(scanId);
    }
}
