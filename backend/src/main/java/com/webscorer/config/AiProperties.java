package com.webscorer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the optional LLM-backed copilot provider.
 *
 * <p>The API key only ever lives on the backend and is read from the
 * {@code WEBSCORER_AI_API_KEY} environment variable. When it is blank the
 * application transparently falls back to {@link com.webscorer.service.LocalCopilotService}
 * and labels every answer as local guidance.</p>
 */
@Configuration
public class AiProperties {

    @Value("${webscorer.ai.api-key:}")
    private String apiKey;

    @Value("${webscorer.ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${webscorer.ai.model:gpt-4o-mini}")
    private String model;

    @Value("${webscorer.ai.timeout-ms:20000}")
    private int timeoutMs;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }
}
