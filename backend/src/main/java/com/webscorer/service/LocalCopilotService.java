package com.webscorer.service;

import com.webscorer.domain.Category;
import com.webscorer.domain.CheckStatus;
import com.webscorer.domain.Severity;
import com.webscorer.dto.CategoryScoreResponse;
import com.webscorer.dto.FindingResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Rule-based copilot used when no AI API key is configured - and as a safety
 * net when the configured provider fails.
 *
 * <p>Every answer is derived from the stored report of the current scan. It
 * never invents findings, never claims a fix was applied and never promises
 * rankings. Answers are labelled so nobody mistakes them for an LLM reply.</p>
 */
@Service
public class LocalCopilotService implements CopilotService {

    static final String PROVIDER_LABEL = "LOCAL";

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "What should I fix first?",
            "Explain my website score",
            "Explain the SEO findings",
            "Generate an example code fix",
            "What does this finding mean for users?");

    @Override
    public String providerName() {
        return PROVIDER_LABEL;
    }

    @Override
    public CopilotAnswer answer(CopilotContext context, String question) {
        String q = question.toLowerCase(Locale.ROOT).trim();

        String guardrail = scopeAnswer(q);
        if (guardrail != null) {
            return local(guardrail);
        }
        if (containsAny(q, "fix first", "what should i fix", "priority", "priorit", "top issue", "start with")) {
            return local(priorityAnswer(context));
        }
        String mentioned = mentionedFinding(context, question);
        if (mentioned != null) {
            return local(specificFindingAnswer(context, mentioned));
        }
        if (containsAny(q, "explain my", "website score", "my score", "how is the score", "why is the score", "overall score")) {
            return local(scoreAnswer(context));
        }
        if (q.contains("seo")) {
            return local(categoryAnswer(context, Category.SEO, "SEO"));
        }
        if (containsAny(q, "accessib", "a11y", "screen reader")) {
            return local(categoryAnswer(context, Category.ACCESSIBILITY, "accessibility"));
        }
        if (containsAny(q, "technical", "https", "viewport", "charset")) {
            return local(categoryAnswer(context, Category.TECHNICAL, "technical"));
        }
        if (containsAny(q, "content structure", "headings", "word count", "navigation")) {
            return local(categoryAnswer(context, Category.CONTENT, "content structure"));
        }
        if (containsAny(q, "code", "snippet", "example fix", "show me the fix", "how do i fix", "how to fix")) {
            return local(codeAnswer(context));
        }
        if (containsAny(q, "mean for users", "user impact", "why does this matter", "for users", "impact")) {
            return local(userImpactAnswer(context));
        }
        if (containsAny(q, "what did you find", "summary", "about my site", "tell me about")) {
            return local(summaryAnswer(context));
        }
        return local(fallbackAnswer(context));
    }

    // ------------------------------------------------------------- intents

    /**
     * The "Ask Webby" buttons quote the finding title in the question; when a
     * title from this report is present we answer about that exact finding
     * instead of guessing which one the user meant.
     */
    private String mentionedFinding(CopilotContext context, String question) {
        String q = question.toLowerCase(Locale.ROOT);
        String match = null;
        for (FindingResponse finding : context.findings()) {
            String title = finding.title().toLowerCase(Locale.ROOT);
            if (q.contains(title) && (match == null || title.length() > match.length())) {
                match = finding.title();
            }
        }
        return match;
    }

    private String specificFindingAnswer(CopilotContext context, String title) {
        FindingResponse finding = context.findings().stream()
                .filter(item -> item.title().equals(title))
                .findFirst()
                .orElse(null);
        if (finding == null) {
            return fallbackAnswer(context);
        }

        StringBuilder builder = new StringBuilder();
        builder.append('"').append(finding.title()).append("\"\n\n");

        if (finding.status() == CheckStatus.PASS) {
            builder.append("Status: PASSED - the scan found no problem here.\n");
        } else if (finding.status() == CheckStatus.NOT_APPLICABLE) {
            builder.append("Status: NOT APPLICABLE - this check does not apply to this page, so it is not counted against you.\n");
        } else if (finding.status() == CheckStatus.NOT_RUN) {
            builder.append("Status: NOT RUN - there was no evidence to evaluate, so it neither helps nor hurts your score.\n");
        } else {
            builder.append("Status: ISSUE (").append(finding.severity()).append(", ")
                    .append(finding.category().getLabel()).append(")\n");
        }

        builder.append("\nWhat the scan saw: ").append(finding.evidence()).append('\n');
        if (finding.explanation() != null && !finding.explanation().isBlank()) {
            builder.append("\nWhy it matters: ").append(finding.explanation()).append('\n');
        }
        if (finding.status() == CheckStatus.FAIL && finding.recommendation() != null
                && !finding.recommendation().isBlank()) {
            builder.append("\nRecommended fix: ").append(finding.recommendation()).append('\n');
        }
        if (finding.codeExample() != null && !finding.codeExample().isBlank()) {
            builder.append("\nExample to adapt:\n```\n").append(finding.codeExample()).append("\n```\n");
        }
        builder.append("\nCheck id: ").append(finding.checkId())
                .append(" - I have not changed anything on your site; review the fix before deploying it.");
        return builder.toString();
    }

    private String scopeAnswer(String q) {
        if (containsAny(q, "guarantee", "guaranteed", "rank #1", "first result on google", "how will i rank",
                "ranking position", "top of google", "seo ranking")) {
            return "I can't promise search rankings - nobody honestly can, and this scan does not talk to Google.\n\n"
                    + "What I *can* tell you is which concrete on-page issues this scan actually found. Fix those and "
                    + "you remove measurable obstacles; ranking outcomes then depend on content, links and competition.\n\n"
                    + "Ask me \"What should I fix first?\" and I will give you the highest-priority issue from your report.";
        }
        if (containsAny(q, "security", "secure", "vulnerab", "penetration", "hack", "xss", "sql injection")) {
            return "This scan only reads public HTML - it is not a security audit and does not test for vulnerabilities.\n\n"
                    + "The one security signal it does measure is transport security: whether the page is served over "
                    + "HTTPS and whether it loads insecure subresources. Ask \"Explain the technical findings\" for those.";
        }
        if (containsAny(q, "performance", "page speed", "loading time", "core web vitals", "lighthouse")) {
            return "I did not measure performance in this scan, so I won't invent a speed score.\n\n"
                    + "Performance data is only shown when a real measurement service is configured for this app. "
                    + "Everything else in your report - SEO, accessibility basics, technical HTML and content structure - "
                    + "comes from the HTML that was actually downloaded.";
        }
        if (containsAny(q, "who are you", "are you an ai", "are you chatgpt", "which model")) {
            return "I'm Webby, the WebScorer AI copilot. Right now I'm running in local guidance mode: answers are "
                    + "assembled from your saved report and documented rules, not from a language model. "
                    + "If an AI provider key is configured on the server, the app switches to it and labels replies accordingly.";
        }
        return null;
    }

    private String priorityAnswer(CopilotContext context) {
        List<FindingResponse> issues = sortedIssues(context);
        if (issues.isEmpty()) {
            return "Nothing urgent - every check that ran for " + context.host() + " passed.\n\n"
                    + "You can re-scan after your next change to confirm the scores hold, or ask "
                    + "\"Explain my website score\" to see how each category was calculated.";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("I'd start with this one for ").append(context.host()).append(":\n\n");
        FindingResponse first = issues.get(0);
        builder.append("1. ").append(first.title())
                .append(" (").append(first.severity()).append(", ").append(first.category().getLabel()).append(")\n")
                .append("   Evidence: ").append(first.evidence()).append("\n")
                .append("   Why it matters: ").append(first.explanation()).append("\n")
                .append("   Fix: ").append(first.recommendation()).append("\n");

        for (int i = 1; i < Math.min(issues.size(), 4); i++) {
            FindingResponse issue = issues.get(i);
            builder.append(i + 1).append(". ").append(issue.title())
                    .append(" (").append(issue.severity()).append(", ").append(issue.category().getLabel()).append(")\n");
        }
        builder.append("\nFix them roughly in this order - severity first, then the categories that carry the most ")
                .append("weight in your score (SEO 35%, Accessibility 25%, Technical 25%, Content 15%).");
        if (first.codeExample() != null) {
            builder.append("\n\nAsk \"Generate an example code fix\" and I'll write out the snippet for #1.");
        }
        return builder.toString();
    }

    private String scoreAnswer(CopilotContext context) {
        StringBuilder builder = new StringBuilder();
        if (context.overallScore() == null) {
            builder.append("There isn't enough evidence for a reliable overall score on this scan, so the app shows ")
                    .append("\"Not enough data\" instead of a number. Category scores that did have enough checks are ")
                    .append("listed below.\n\n");
        } else {
            builder.append("Your overall score is ").append(context.overallScore()).append("/100.\n\n");
            if (!context.historyScores().isEmpty()) {
                builder.append("Previous scans of the same URL: ");
                List<String> parts = new ArrayList<>();
                context.historyScores().forEach((when, score) -> parts.add(score + " on " + when));
                builder.append(String.join(", ", parts)).append(".\n\n");
            }
        }
        builder.append("How it is calculated:\n");
        for (CategoryScoreResponse score : context.categoryScores()) {
            if (score.score() == null) {
                builder.append("- ").append(score.label()).append(": not enough data (only ")
                        .append(score.checksApplicable()).append(" of ").append(score.checksDefined())
                        .append(" checks could be evaluated).\n");
            } else {
                builder.append("- ").append(score.label()).append(": ").append(score.score())
                        .append("/100 - ").append(score.checksPassed()).append(" of ")
                        .append(score.checksApplicable()).append(" applicable checks passed.\n");
            }
        }
        builder.append("\nThe overall number is the weighted mean of those category scores (SEO 35%, Accessibility 25%, ")
                .append("Technical 25%, Content 15%). Checks that could not run are excluded rather than counted as ")
                .append("failures, so missing evidence never lowers your score.");
        return builder.toString();
    }

    private String categoryAnswer(CopilotContext context, Category category, String label) {
        List<FindingResponse> issues = context.issuesIn(category);
        List<CategoryScoreResponse> score = context.categoryScores().stream()
                .filter(s -> s.category() == category)
                .toList();

        StringBuilder builder = new StringBuilder();
        builder.append("Here's what the scan found for ").append(label).append(":\n\n");
        if (score.isEmpty()) {
            builder.append("This category was not scored for the current scan.\n");
        } else if (score.get(0).score() == null) {
            builder.append("Score: not enough data (").append(score.get(0).checksApplicable()).append(" of ")
                    .append(score.get(0).checksDefined()).append(" checks could be evaluated).\n");
        } else {
            builder.append("Score: ").append(score.get(0).score()).append("/100 - ")
                    .append(score.get(0).checksPassed()).append(" of ").append(score.get(0).checksApplicable())
                    .append(" applicable checks passed.\n");
        }

        if (issues.isEmpty()) {
            long passed = context.findings().stream()
                    .filter(f -> f.category() == category && f.status() == CheckStatus.PASS).count();
            builder.append("\nNo ").append(label).append(" issues detected - ").append(passed)
                    .append(" check(s) passed in this category.");
            return builder.toString();
        }

        builder.append("\nIssues found:\n");
        int index = 1;
        for (FindingResponse issue : issues) {
            builder.append(index++).append(". ").append(issue.title())
                    .append(" (").append(issue.severity()).append(")\n")
                    .append("   Evidence: ").append(issue.evidence()).append("\n")
                    .append("   Fix: ").append(issue.recommendation()).append("\n\n");
        }
        builder.append("I can't promise these are all the ").append(label)
                .append(" problems a full audit would find - this is what the fetched HTML shows.");
        return builder.toString();
    }

    private String codeAnswer(CopilotContext context) {
        List<FindingResponse> withCode = sortedIssues(context).stream()
                .filter(f -> f.codeExample() != null && !f.codeExample().isBlank())
                .toList();
        if (withCode.isEmpty()) {
            return "None of the open issues in this report has a ready-made snippet - they are mostly content or "
                    + "configuration changes rather than markup edits.\n\nPick any finding in the report and use "
                    + "\"Ask Webby\" on it; I'll walk through the change step by step.";
        }
        FindingResponse target = withCode.get(0);
        return "Illustrative fix for \"" + target.title() + "\":\n\n"
                + "```\n" + target.codeExample() + "\n```\n\n"
                + "Context from your scan: " + target.evidence() + "\n\n"
                + "This snippet is an example to adapt - I have not applied anything to your site, and you should "
                + "review it against your own templates before shipping.";
    }

    private String userImpactAnswer(CopilotContext context) {
        List<FindingResponse> issues = sortedIssues(context);
        if (issues.isEmpty()) {
            return "No failing checks were recorded for this scan, so there is no user-facing problem to explain yet.";
        }
        FindingResponse top = issues.get(0);
        return "\"" + top.title() + "\" - what it means for real visitors:\n\n"
                + top.explanation() + "\n\n"
                + "What the scan actually saw: " + top.evidence() + "\n\n"
                + "In short: " + impactSentence(top);
    }

    private String summaryAnswer(CopilotContext context) {
        long issues = context.issues().size();
        long passed = context.findings().stream().filter(f -> f.status() == CheckStatus.PASS).count();
        StringBuilder builder = new StringBuilder();
        builder.append("Summary for ").append(context.host()).append(":\n\n");
        if (context.overallScore() != null) {
            builder.append("Overall score ").append(context.overallScore()).append("/100. ");
        }
        builder.append(passed).append(" checks passed, ").append(issues).append(" issue(s) recorded.\n\n");
        if (context.summary() != null && !context.summary().isBlank()) {
            builder.append(context.summary()).append("\n\n");
        }
        if (issues == 0) {
            builder.append("Nothing to fix was detected by these checks.");
        } else {
            builder.append("Top issues:\n");
            List<FindingResponse> top = sortedIssues(context).stream().limit(3).toList();
            for (int i = 0; i < top.size(); i++) {
                FindingResponse issue = top.get(i);
                builder.append(i + 1).append(". ").append(issue.title()).append(" (")
                        .append(issue.severity()).append(", ").append(issue.category().getLabel()).append(")\n");
            }
            builder.append("\nAsk \"What should I fix first?\" for the detailed plan.");
        }
        return builder.toString();
    }

    private String fallbackAnswer(CopilotContext context) {
        return "I answer questions about the report for " + context.host() + " - I don't have access to anything else "
                + "(no server files, no analytics, no other pages).\n\n"
                + "Try one of these:\n"
                + "- What should I fix first?\n"
                + "- Explain my website score\n"
                + "- Explain the SEO findings\n"
                + "- Generate an example code fix\n"
                + "- What does this finding mean for users?\n\n"
                + summaryLine(context);
    }

    private String summaryLine(CopilotContext context) {
        long issues = context.issues().size();
        return "Report snapshot: " + context.findings().size() + " checks recorded, " + issues + " issue(s)"
                + (context.overallScore() != null ? ", overall score " + context.overallScore() + "/100."
                        : ", overall score not available (not enough data).");
    }

    private String impactSentence(FindingResponse issue) {
        return switch (issue.severity()) {
            case CRITICAL -> "visitors may see a browser warning or the page may be modified in transit before it "
                    + "reaches them.";
            case HIGH -> "people using assistive technology, or search engines, may miss this content entirely.";
            case MEDIUM -> "the page still works, but visitors get less guidance and the signal is weaker.";
            case LOW, INFO -> "a small polish issue that adds up when several appear together.";
        };
    }

    // ------------------------------------------------------------- helpers

    List<FindingResponse> sortedIssues(CopilotContext context) {
        return context.issues().stream()
                .sorted(Comparator.comparingInt((FindingResponse f) -> f.severity().getRank()).reversed()
                        .thenComparingInt((FindingResponse f) -> -(int) Math.round(
                                ScoringService.CATEGORY_WEIGHTS.getOrDefault(f.category(), 0.0d) * 100))
                        .thenComparing(FindingResponse::title))
                .toList();
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private CopilotAnswer local(String message) {
        return new CopilotAnswer(message, PROVIDER_LABEL,
                "Answered from this scan's saved report using built-in rules - no AI key is configured.");
    }

    public static List<String> defaultSuggestions() {
        return DEFAULT_SUGGESTIONS;
    }
}
