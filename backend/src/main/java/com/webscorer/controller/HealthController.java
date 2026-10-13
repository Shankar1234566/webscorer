package com.webscorer.controller;

import com.webscorer.service.CopilotRouter;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    public record HealthResponse(
            String status,
            String application,
            Instant timestamp,
            boolean aiConfigured,
            String copilotMode
    ) {
    }

    private final CopilotRouter router;

    public HealthController(CopilotRouter router) {
        this.router = router;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "webscorer-ai", Instant.now(),
                router.isAiConfigured(), router.isAiConfigured() ? "llm" : "local-guidance");
    }
}
