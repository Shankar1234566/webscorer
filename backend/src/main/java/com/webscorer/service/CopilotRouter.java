package com.webscorer.service;

import com.webscorer.config.AiProperties;
import org.springframework.stereotype.Service;

/**
 * Chooses the copilot provider at request time.
 *
 * <ul>
 *   <li>No API key configured -> {@link LocalCopilotService}, labelled LOCAL.</li>
 *   <li>API key configured -> {@link LlmCopilotService}; if the provider fails
 *       the router transparently falls back to local guidance and says so in
 *       the note, so a fallback is never mistaken for an AI reply.</li>
 * </ul>
 */
@Service
public class CopilotRouter implements CopilotService {

    private final AiProperties properties;
    private final LocalCopilotService local;
    private final LlmCopilotService llm;

    public CopilotRouter(AiProperties properties, LocalCopilotService local, LlmCopilotService llm) {
        this.properties = properties;
        this.local = local;
        this.llm = llm;
    }

    @Override
    public String providerName() {
        return properties.isEnabled() ? llm.providerName() : local.providerName();
    }

    public boolean isAiConfigured() {
        return properties.isEnabled();
    }

    @Override
    public CopilotAnswer answer(CopilotContext context, String question) {
        if (!properties.isEnabled()) {
            return local.answer(context, question);
        }
        try {
            return llm.answer(context, question);
        } catch (RuntimeException e) {
            CopilotAnswer fallback = local.answer(context, question);
            String reason = e.getMessage() == null ? "provider error" : e.getMessage();
            return new CopilotAnswer(fallback.message(), local.providerName(),
                    "The AI provider was unavailable (" + reason + "), so this answer came from local guidance mode.");
        }
    }
}
