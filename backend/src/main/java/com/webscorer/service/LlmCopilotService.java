package com.webscorer.service;

import com.webscorer.config.AiProperties;
import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import com.webscorer.dto.CategoryScoreResponse;
import com.webscorer.dto.FindingResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * LLM-backed copilot provider (OpenAI-compatible chat completions API).
 *
 * <p>The API key stays on the server, read from {@code WEBSCORER_AI_API_KEY}.
 * Only <em>report metadata</em> is sent - scores, findings, evidence - never
 * the downloaded page body. Any transport or provider failure throws, and
 * {@link CopilotRouter} falls back to local guidance.</p>
 */
@Service
public class LlmCopilotService implements CopilotService {

    private static final Logger log = LoggerFactory.getLogger(LlmCopilotService.class);

    private static final String SYSTEM_PROMPT = """
            You are Webby, the copilot inside WebScorer AI, a website auditing tool.
            You help developers and students understand a website scan report and decide what to fix first.

            Rules you must follow:
            - Use ONLY the report context supplied by the user. Never invent findings, URLs, metrics or evidence.
            - If the report does not contain the answer, say so plainly and suggest what the user could scan or check.
            - Never claim that a fix was applied automatically; suggestions are examples the user must review and deploy.
            - Never guarantee search rankings, traffic, security or performance results.
            - Be friendly and concise, use plain language, prefer short paragraphs and numbered steps.
            - When you show code, keep it minimal and clearly mark it as an example to adapt.
            - Distinguish clearly between "this check failed" and "this check could not be run".
            """;

    private final AiProperties properties;
    private final HttpClient httpClient;

    public LlmCopilotService(AiProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(2000, properties.getTimeoutMs())))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public String providerName() {
        return "LLM";
    }

    @Override
    public CopilotAnswer answer(CopilotContext context, String question) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("No AI API key is configured.");
        }
        try {
            String payload = buildPayload(context, question);
            String endpoint = endpoint();

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("The AI provider returned HTTP " + response.statusCode());
            }
            String text = extractContent(response.body());
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("The AI provider returned an empty reply.");
            }
            return new CopilotAnswer(text.trim(), "LLM", null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The AI request was interrupted.", e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("LLM copilot call failed: {}", e.getMessage());
            throw new IllegalStateException("The AI provider could not be reached: " + e.getMessage(), e);
        }
    }

    private String endpoint() {
        String base = properties.getBaseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }

    private String buildPayload(CopilotContext context, String question) {
        String user = "REPORT CONTEXT\n"
                + "URL: " + context.url() + "\n"
                + "Overall score: " + (context.overallScore() == null ? "not enough data" : context.overallScore() + "/100") + "\n"
                + "Summary: " + nullToDash(context.summary()) + "\n"
                + "Category scores:\n" + categoryLines(context) + "\n"
                + "Checks (" + context.findings().size() + "):\n" + findingLines(context) + "\n"
                + "Previous scans: " + (context.historyScores().isEmpty() ? "none" : context.historyScores().toString()) + "\n\n"
                + "USER QUESTION\n" + question;

        // Escape for embedding in a JSON string without pulling in a JSON library.
        String escapedSystem = jsonEscape(SYSTEM_PROMPT);
        String escapedUser = jsonEscape(user);
        String escapedModel = jsonEscape(properties.getModel());
        return "{\"model\":\"" + escapedModel + "\",\"temperature\":0.3,\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapedSystem + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapedUser + "\"}]}";
    }

    private String categoryLines(CopilotContext context) {
        return context.categoryScores().stream()
                .map(s -> "- " + s.label() + ": " + (s.score() == null
                        ? "not enough data"
                        : s.score() + "/100 (" + s.checksPassed() + "/" + s.checksApplicable() + " passed)"))
                .collect(Collectors.joining("\n"));
    }

    private String findingLines(CopilotContext context) {
        List<FindingResponse> findings = context.findings();
        if (findings.isEmpty()) {
            return "- none recorded";
        }
        return findings.stream()
                .limit(60)
                .map(f -> "- [" + f.category().getLabel() + "][" + f.status() + "]["
                        + (f.status() == CheckStatus.FAIL ? f.severity() : "-") + "] " + f.title()
                        + " | evidence: " + trim(f.evidence(), 220)
                        + (f.status() == CheckStatus.FAIL ? " | fix: " + trim(f.recommendation(), 220) : ""))
                .collect(Collectors.joining("\n"));
    }

    /** Minimal JSON string escaping for the two control characters that matter. */
    private static String jsonEscape(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.toString();
    }

    private static String extractContent(String body) {
        // {"choices":[{"message":{"content":"..."}}]}
        int contentIndex = body.indexOf("\"content\"");
        if (contentIndex < 0) {
            return null;
        }
        int colon = body.indexOf(':', contentIndex);
        int start = body.indexOf('"', colon + 1);
        if (start < 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start + 1; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                char next = body.charAt(++i);
                switch (next) {
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case 'u' -> {
                        if (i + 4 < body.length()) {
                            builder.append((char) Integer.parseInt(body.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                    }
                    default -> builder.append(next);
                }
                continue;
            }
            if (c == '"') {
                break;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String clean = value.replaceAll("\\s+", " ").trim();
        return clean.length() <= max ? clean : clean.substring(0, max - 1) + "…";
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
