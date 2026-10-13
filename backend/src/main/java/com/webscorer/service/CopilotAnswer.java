package com.webscorer.service;

/**
 * Answer produced by a copilot provider.
 *
 * @param message  the reply shown to the user
 * @param provider "LOCAL" (rule-based) or "LLM" (configured AI provider)
 * @param note     optional extra context such as a fallback explanation
 */
public record CopilotAnswer(String message, String provider, String note) {
}
