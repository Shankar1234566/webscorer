package com.webscorer.controller;

import com.webscorer.dto.CopilotRequest;
import com.webscorer.dto.CopilotResponse;
import com.webscorer.dto.MessageResponse;
import com.webscorer.service.CopilotChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Chat endpoints for Webby, always scoped to one scan's report. */
@RestController
@RequestMapping("/api/scans/{scanId}/copilot")
public class CopilotController {

    private final CopilotChatService chatService;

    public CopilotController(CopilotChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public CopilotResponse ask(@PathVariable Long scanId, @Valid @RequestBody CopilotRequest request) {
        return chatService.ask(scanId, request.question());
    }

    @GetMapping
    public List<MessageResponse> history(@PathVariable Long scanId) {
        return chatService.history(scanId);
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@PathVariable Long scanId) {
        chatService.clear(scanId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
