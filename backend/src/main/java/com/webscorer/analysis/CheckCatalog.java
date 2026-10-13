package com.webscorer.analysis;

import com.webscorer.domain.Category;
import com.webscorer.domain.Severity;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The complete, documented check catalog (25 checks).
 * Rules and weights are also described in docs/scoring-rules.md - the two must
 * stay in sync, which the ScoringServiceTest guards against.
 */
public final class CheckCatalog {

    private CheckCatalog() {
    }

    // ---------------------------------------------------------------- SEO ---
    public static final CheckDefinition SEO_TITLE_PRESENT = new CheckDefinition(
            "seo.title.present", Category.SEO, Severity.HIGH, 1,
            "Page has a title",
            "The <title> is the first thing people read in search results, tabs and shared links. "
                    + "Search engines also use it to understand what the page is about.",
            "Add a unique, descriptive <title> of roughly 30-60 characters inside <head>.",
            "<title>Your main topic — Brand name</title>");

    public static final CheckDefinition SEO_TITLE_LENGTH = new CheckDefinition(
            "seo.title.length", Category.SEO, Severity.MEDIUM, 1,
            "Title length is a sensible size",
            "Titles under 10 characters carry little context; titles over 70 characters get cut off "
                    + "in search results, which hides your brand and can look spammy.",
            "Keep the title between 10 and 70 characters, most important words first.",
            "<title>Fast React component library — Acme UI</title>");

    public static final CheckDefinition SEO_DESCRIPTION_PRESENT = new CheckDefinition(
            "seo.description.present", Category.SEO, Severity.HIGH, 1,
            "Meta description exists",
            "The meta description is your free ad copy in search results. Without it search engines "
                    + "guess a snippet, which usually lowers click-through.",
            "Add a meta description that summarises the page in one or two sentences.",
            "<meta name=\"description\" content=\"Explain what this page offers and who it is for.\">");

    public static final CheckDefinition SEO_DESCRIPTION_LENGTH = new CheckDefinition(
            "seo.description.length", Category.SEO, Severity.MEDIUM, 1,
            "Meta description length is a sensible size",
            "Descriptions shorter than ~50 characters waste space; longer than ~160 characters are "
                    + "truncated in results, cutting off the call to action.",
            "Aim for 50-160 characters that describe the page honestly.",
            "<meta name=\"description\" content=\"Scan any website for SEO, accessibility and technical "
                    + "issues and get a prioritised fix list.\">");

    public static final CheckDefinition SEO_H1_PRESENT = new CheckDefinition(
            "seo.h1.present", Category.SEO, Severity.MEDIUM, 1,
            "Page has one primary heading (H1)",
            "The H1 tells both visitors and crawlers what the page is. Pages without one are harder "
                    + "to skim and weaker contextually for search engines.",
            "Add exactly one <h1> that states the page topic, then nest <h2>/<h3> underneath.",
            "<h1>Website quality scanner</h1>");

    public static final CheckDefinition SEO_CANONICAL = new CheckDefinition(
            "seo.canonical.present", Category.SEO, Severity.MEDIUM, 1,
            "Canonical URL is declared",
            "A canonical link tells search engines which URL is the real one, preventing duplicate "
                    + "content from splitting your ranking signals.",
            "Add a self-referencing canonical link for normal pages.",
            "<link rel=\"canonical\" href=\"https://example.com/pricing\">");

    public static final CheckDefinition SEO_ROBOTS_INDEXABLE = new CheckDefinition(
            "seo.robots.indexable", Category.SEO, Severity.HIGH, 1,
            "Page is indexable",
            "A noindex directive removes the page from search results entirely, even if everything "
                    + "else is perfect. It is sometimes left in place by accident after testing.",
            "Remove content=\"noindex\" once the page is ready to be published.",
            "<meta name=\"robots\" content=\"index, follow\">");

    public static final CheckDefinition SEO_IMAGE_ALT = new CheckDefinition(
            "seo.image.alt", Category.SEO, Severity.MEDIUM, 1,
            "Images carry alt text (search visibility)",
            "Alt text gives image search a text description of the picture and is how your images "
                    + "appear in image results.",
            "Describe each informative image in a few words inside its alt attribute.",
            "<img src=\"dashboard.png\" alt=\"WebScorer report dashboard with an overall score\">");

    // ------------------------------------------------------ ACCESSIBILITY ---
    public static final CheckDefinition A11Y_IMAGES_ALT = new CheckDefinition(
            "a11y.images.alt", Category.ACCESSIBILITY, Severity.HIGH, 1,
            "Images have alt attributes",
            "Screen reader users hear alt text instead of seeing the image. Without it they hear the "
                    + "file name, and people using voice control cannot reference the picture.",
            "Add alt text to informative images; use alt=\"\" for purely decorative ones.",
            "<img src=\"team-photo.jpg\" alt=\"The four members of the design team\">");

    public static final CheckDefinition A11Y_FORM_LABELS = new CheckDefinition(
            "a11y.form.labels", Category.ACCESSIBILITY, Severity.HIGH, 1,
            "Form fields have associated labels",
            "A visible label plus a programmatic association is what lets a screen reader announce "
                    + "'Email, edit box' when a field receives focus.",
            "Give each input an id and a matching <label for>, or an aria-label.",
            "<label for=\"email\">Email</label>\n<input id=\"email\" type=\"email\" name=\"email\">");

    public static final CheckDefinition A11Y_LINK_NAMES = new CheckDefinition(
            "a11y.links.names", Category.ACCESSIBILITY, Severity.MEDIUM, 1,
            "Links have a meaningful accessible name",
            "Screen readers list links out of context. 'Link' or a bare URL gives no clue where it "
                    + "goes, so keyboard users have to open candidates one by one.",
            "Give every link visible text that describes its destination, or an aria-label.",
            "<a href=\"/pricing\">See pricing plans</a>");

    public static final CheckDefinition A11Y_BUTTON_NAMES = new CheckDefinition(
            "a11y.button.names", Category.ACCESSIBILITY, Severity.MEDIUM, 1,
            "Buttons have an accessible name",
            "Icon-only buttons render as an anonymous control for assistive technology, which makes "
                    + "the main action of a page impossible to find.",
            "Add visible text or an aria-label describing what the button does.",
            "<button type=\"submit\" aria-label=\"Run website scan\">…</button>");

    public static final CheckDefinition A11Y_LANG = new CheckDefinition(
            "a11y.lang", Category.ACCESSIBILITY, Severity.HIGH, 1,
            "Document declares its language",
            "The lang attribute decides which pronunciation a screen reader uses. The wrong language "
                    + "makes every sentence unreadable to the listener.",
            "Set the language on the <html> element to match the page content.",
            "<html lang=\"en\">");

    public static final CheckDefinition A11Y_IFRAME_TITLE = new CheckDefinition(
            "a11y.iframe.title", Category.ACCESSIBILITY, Severity.LOW, 1,
            "Iframes have a title",
            "An embedded frame without a title is announced as an unlabelled frame, so users cannot "
                    + "tell a map apart from a payment form.",
            "Add a short title attribute to every <iframe>.",
            "<iframe src=\"…\" title=\"Store location map\"></iframe>");

    // ----------------------------------------------------------- TECHNICAL ---
    public static final CheckDefinition TECH_HTTPS = new CheckDefinition(
            "tech.https", Category.TECHNICAL, Severity.CRITICAL, 1,
            "Site is served over HTTPS",
            "Plain HTTP lets anyone on the network read or modify the page in transit, and browsers "
                    + "flag it as 'Not secure', which scares visitors away.",
            "Serve the site over HTTPS and redirect every HTTP request to HTTPS.",
            "server {\n  listen 80;\n  return 301 https://$host$request_uri;\n}");

    public static final CheckDefinition TECH_RETRIEVABLE = new CheckDefinition(
            "tech.retrievable", Category.TECHNICAL, Severity.CRITICAL, 1,
            "HTML document is retrievable",
            "If the document cannot be downloaded reliably, nothing else about the page can be "
                    + "verified - this check anchors every other result in the report.",
            "Check hosting, DNS and any rate limiting that may block automated requests.",
            null);

    public static final CheckDefinition TECH_STRUCTURE = new CheckDefinition(
            "tech.structure", Category.TECHNICAL, Severity.MEDIUM, 1,
            "Document has a parseable structure",
            "A document missing html/head/body is ambiguous to parse, so browsers and crawlers fall "
                    + "back to guesswork that differs between engines.",
            "Emit a full document skeleton with a doctype.",
            "<!doctype html>\n<html lang=\"en\">\n  <head><meta charset=\"utf-8\"><title>…</title></head>\n  <body>…</body>\n</html>");

    public static final CheckDefinition TECH_VIEWPORT = new CheckDefinition(
            "tech.viewport", Category.TECHNICAL, Severity.MEDIUM, 1,
            "Viewport meta tag present",
            "Without a viewport declaration mobile browsers render a desktop layout zoomed out, so "
                    + "text is tiny and tap targets overlap.",
            "Add the standard responsive viewport meta tag.",
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");

    public static final CheckDefinition TECH_CHARSET = new CheckDefinition(
            "tech.charset", Category.TECHNICAL, Severity.LOW, 1,
            "Character encoding declared",
            "A declared charset prevents accented characters, emoji and non-Latin scripts from "
                    + "showing up as mojibake.",
            "Declare UTF-8 as early as possible in <head>.",
            "<meta charset=\"utf-8\">");

    public static final CheckDefinition TECH_LINK_TARGETS = new CheckDefinition(
            "tech.links.targets", Category.TECHNICAL, Severity.MEDIUM, 1,
            "Links have usable destinations",
            "Empty, '#' or javascript: href values look clickable but do nothing, which strands "
                    + "keyboard and screen reader users on a dead control.",
            "Point each anchor at a real URL, or use a <button> if the action is not navigation.",
            "<a href=\"/docs/getting-started\">Getting started</a>");

    public static final CheckDefinition TECH_MIXED_CONTENT = new CheckDefinition(
            "tech.mixed.content", Category.TECHNICAL, Severity.MEDIUM, 1,
            "No insecure subresources on an HTTPS page",
            "Browsers block or warn about HTTP images, scripts and styles on an HTTPS page, which "
                    + "can silently break layout and functionality.",
            "Reference every subresource with HTTPS (protocol-relative or absolute).",
            "<script src=\"https://cdn.example.com/app.js\"></script>");

    // ------------------------------------------------------------ CONTENT ---
    public static final CheckDefinition CONTENT_TEXT = new CheckDefinition(
            "content.text", Category.CONTENT, Severity.MEDIUM, 1,
            "Page has meaningful text content",
            "Visitors and search engines both need enough copy to understand what the page offers. "
                    + "A near-empty page offers nothing to read, rank or share.",
            "Write at least ~100 words that answer the visitor's question on this page.",
            null);

    public static final CheckDefinition CONTENT_HEADINGS = new CheckDefinition(
            "content.headings", Category.CONTENT, Severity.LOW, 1,
            "Headings divide the content",
            "Headings act as a table of sight: they let readers scan for the section they need and "
                    + "let assistive technology jump between sections.",
            "Break long copy into sections with <h2> headings that follow on from the H1.",
            "<h2>What you get</h2>\n<p>…</p>\n<h2>How it works</h2>\n<p>…</p>");

    public static final CheckDefinition CONTENT_NAV = new CheckDefinition(
            "content.navigation", Category.CONTENT, Severity.LOW, 1,
            "Navigation can be identified",
            "A recognisable navigation landmark lets screen reader users skip straight to the menus "
                    + "instead of reading the whole page top to bottom.",
            "Wrap primary links in a <nav> element with an accessible label.",
            "<nav aria-label=\"Primary\">…</nav>");

    public static final CheckDefinition CONTENT_LINK_TEXT = new CheckDefinition(
            "content.link.text", Category.CONTENT, Severity.LOW, 1,
            "Link text is descriptive",
            "Links such as 'click here' are meaningless when read alone, forcing screen reader users "
                    + "to open each one to discover its destination.",
            "Rewrite generic links so the text itself says where they go.",
            "<a href=\"/reports\">Read the full audit report</a>");

    public static final List<CheckDefinition> ALL = List.of(
            SEO_TITLE_PRESENT, SEO_TITLE_LENGTH, SEO_DESCRIPTION_PRESENT, SEO_DESCRIPTION_LENGTH,
            SEO_H1_PRESENT, SEO_CANONICAL, SEO_ROBOTS_INDEXABLE, SEO_IMAGE_ALT,
            A11Y_IMAGES_ALT, A11Y_FORM_LABELS, A11Y_LINK_NAMES, A11Y_BUTTON_NAMES,
            A11Y_LANG, A11Y_IFRAME_TITLE,
            TECH_HTTPS, TECH_RETRIEVABLE, TECH_STRUCTURE, TECH_VIEWPORT, TECH_CHARSET,
            TECH_LINK_TARGETS, TECH_MIXED_CONTENT,
            CONTENT_TEXT, CONTENT_HEADINGS, CONTENT_NAV, CONTENT_LINK_TEXT);

    private static final Map<String, CheckDefinition> BY_ID = ALL.stream()
            .collect(Collectors.toMap(CheckDefinition::id, Function.identity()));

    private static final Map<Category, List<CheckDefinition>> BY_CATEGORY = ALL.stream()
            .collect(Collectors.groupingBy(CheckDefinition::category, () -> new EnumMap<>(Category.class),
                    Collectors.toList()));

    public static CheckDefinition byId(String id) {
        CheckDefinition definition = BY_ID.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown check id: " + id);
        }
        return definition;
    }

    public static List<CheckDefinition> forCategory(Category category) {
        return Collections.unmodifiableList(BY_CATEGORY.getOrDefault(category, List.of()));
    }

    public static int definedCount(Category category) {
        return forCategory(category).size();
    }
}
