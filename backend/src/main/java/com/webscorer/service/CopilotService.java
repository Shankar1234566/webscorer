package com.webscorer.service;

/**
 * Independent copilot provider abstraction. The application ships two
 * implementations: {@link LocalCopilotService} (always available, rule-based)
 * and {@link LlmCopilotService} (used only when an API key is configured).
 */
public interface CopilotService {

    CopilotAnswer answer(CopilotContext context, String question);

    /** Shown in the UI so the user always knows who is talking. */
    String providerName();
}
