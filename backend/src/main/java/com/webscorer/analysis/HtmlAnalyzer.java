package com.webscorer.analysis;

import com.webscorer.domain.CheckStatus;
import com.webscorer.domain.Severity;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Runs every check in {@link CheckCatalog} against a fetched document.
 *
 * <p>The class is deliberately pure: no network access, no randomness, no
 * clock. The same document always produces the same results, which is what
 * makes the score reproducible.</p>
 */
public class HtmlAnalyzer {

    private static final int TITLE_MIN = 10;
    private static final int TITLE_MAX = 70;
    private static final int DESCRIPTION_MIN = 50;
    private static final int DESCRIPTION_MAX = 160;
    private static final int MIN_WORDS = 100;
    private static final int MAX_GENERIC_LINKS = 2;

    private static final List<String> GENERIC_LINK_TEXTS = List.of(
            "click here", "here", "read more", "more", "learn more", "this", "link", "details", "continue");

    public List<CheckResult> analyze(PageContext ctx) {
        List<CheckResult> results = new ArrayList<>();

        if (ctx == null || ctx.doc() == null) {
            // Nothing to look at: every check is missing evidence, never a failure.
            for (CheckDefinition def : CheckCatalog.ALL) {
                results.add(notRun(def, "The document could not be parsed, so this check was not executed."));
            }
            return results;
        }

        Document doc = ctx.doc();
        results.addAll(seoChecks(ctx, doc));
        results.addAll(accessibilityChecks(ctx, doc));
        results.addAll(technicalChecks(ctx, doc));
        results.addAll(contentChecks(ctx, doc));
        return results;
    }

    // ------------------------------------------------------------------ SEO

    private List<CheckResult> seoChecks(PageContext ctx, Document doc) {
        List<CheckResult> out = new ArrayList<>();
        String title = textOf(doc.selectFirst("title"));

        if (isBlank(title)) {
            out.add(fail(CheckCatalog.SEO_TITLE_PRESENT, "No <title> element with text was found in <head>."));
            out.add(notApplicable(CheckCatalog.SEO_TITLE_LENGTH, "Skipped: there is no title to measure."));
        } else {
            out.add(pass(CheckCatalog.SEO_TITLE_PRESENT,
                    "Title: \"" + clip(title, 90) + "\" (" + title.length() + " characters)."));
            int length = title.length();
            if (length >= TITLE_MIN && length <= TITLE_MAX) {
                out.add(pass(CheckCatalog.SEO_TITLE_LENGTH, "Title length " + length + " characters (target " + TITLE_MIN + "-" + TITLE_MAX + ")."));
            } else {
                out.add(fail(CheckCatalog.SEO_TITLE_LENGTH,
                        "Title length " + length + " characters, outside the recommended " + TITLE_MIN + "-" + TITLE_MAX + " range."));
            }
        }

        Element description = doc.selectFirst("meta[name=description]");
        String descriptionContent = description == null ? null : description.attr("content").trim();
        if (isBlank(descriptionContent)) {
            out.add(fail(CheckCatalog.SEO_DESCRIPTION_PRESENT, "No <meta name=\"description\"> with content was found."));
            out.add(notApplicable(CheckCatalog.SEO_DESCRIPTION_LENGTH, "Skipped: there is no description to measure."));
        } else {
            out.add(pass(CheckCatalog.SEO_DESCRIPTION_PRESENT,
                    "Description found (" + descriptionContent.length() + " characters)."));
            int length = descriptionContent.length();
            if (length >= DESCRIPTION_MIN && length <= DESCRIPTION_MAX) {
                out.add(pass(CheckCatalog.SEO_DESCRIPTION_LENGTH, "Description length " + length + " characters (target " + DESCRIPTION_MIN + "-" + DESCRIPTION_MAX + ")."));
            } else {
                out.add(fail(CheckCatalog.SEO_DESCRIPTION_LENGTH,
                        "Description length " + length + " characters, outside the recommended " + DESCRIPTION_MIN + "-" + DESCRIPTION_MAX + " range."));
            }
        }

        Elements h1s = doc.select("h1");
        long nonEmptyH1 = h1s.stream().filter(h -> !isBlank(h.text())).count();
        if (nonEmptyH1 == 1) {
            out.add(pass(CheckCatalog.SEO_H1_PRESENT, "Exactly one non-empty <h1>: \"" + clip(h1s.first().text(), 70) + "\"."));
        } else if (nonEmptyH1 == 0) {
            out.add(fail(CheckCatalog.SEO_H1_PRESENT, "No non-empty <h1> element found (found " + h1s.size() + " <h1> tags)."));
        } else {
            out.add(fail(CheckCatalog.SEO_H1_PRESENT, nonEmptyH1 + " non-empty <h1> elements found; a page should have one primary heading."));
        }

        Element canonical = doc.selectFirst("link[rel~=canonical]");
        if (canonical == null) {
            out.add(fail(CheckCatalog.SEO_CANONICAL, "No <link rel=\"canonical\"> found in <head>."));
        } else {
            String href = canonical.attr("href").trim();
            if (isBlank(href)) {
                out.add(fail(CheckCatalog.SEO_CANONICAL, "A canonical link exists but its href attribute is empty."));
            } else {
                out.add(pass(CheckCatalog.SEO_CANONICAL, "Canonical declared as " + clip(href, 120) + "."));
            }
        }

        // Crawler-specific directives (googlebot/bingbot) count as well as "robots",
        // matched case-insensitively, because a Google-only noindex still hides the page.
        Element robots = findRobotsMeta(doc);
        String robotsName = robots == null ? "" : robots.attr("name").trim();
        String robotsContent = robots == null ? "" : robots.attr("content").toLowerCase(Locale.ROOT);
        if (robotsContent.contains("noindex") || robotsContent.contains("none")) {
            out.add(fail(CheckCatalog.SEO_ROBOTS_INDEXABLE,
                    "<meta name=\"" + robotsName + "\" content=\"" + clip(robotsContent, 60) + "\"> blocks indexing."));
        } else if (robots == null) {
            out.add(pass(CheckCatalog.SEO_ROBOTS_INDEXABLE,
                    "No meta robots tag - the default (index, follow) applies."));
        } else if (isBlank(robotsContent)) {
            out.add(pass(CheckCatalog.SEO_ROBOTS_INDEXABLE,
                    "A <meta name=\"" + robotsName + "\"> tag exists but carries no content, so it issues no "
                            + "directive and the default (index, follow) applies."));
        } else {
            out.add(pass(CheckCatalog.SEO_ROBOTS_INDEXABLE,
                    "<meta name=\"" + robotsName + "\" content=\"" + clip(robotsContent, 60) + "\"> allows indexing."));
        }

        out.addAll(imageAltResults(ctx, doc, CheckCatalog.SEO_IMAGE_ALT, CheckCatalog.A11Y_IMAGES_ALT));
        return out;
    }

    // -------------------------------------------------------- ACCESSIBILITY

    private List<CheckResult> accessibilityChecks(PageContext ctx, Document doc) {
        List<CheckResult> out = new ArrayList<>();
        // The shared image-alt evidence was produced while running the SEO checks.
        Element lang = doc.selectFirst("html[lang]");
        if (lang != null && !isBlank(lang.attr("lang"))) {
            out.add(pass(CheckCatalog.A11Y_LANG, "<html lang=\"" + lang.attr("lang").trim() + "\"> declared."));
        } else {
            out.add(fail(CheckCatalog.A11Y_LANG, "The <html> element has no lang attribute."));
        }

        // image alt + labels + link/button names
        Element htmlRoot = doc;
        Elements inputs = htmlRoot.select("input, select, textarea");
        List<Element> labelledFields = new ArrayList<>();
        int unnamedFields = 0;
        int skippedFields = 0;
        for (Element input : inputs) {
            String type = input.attr("type").toLowerCase(Locale.ROOT);
            if (type.equals("hidden") || type.equals("submit") || type.equals("button") || type.equals("reset")) {
                skippedFields++;
                continue;
            }
            labelledFields.add(input);
            if (!hasAccessibleName(input)) {
                unnamedFields++;
            }
        }
        if (labelledFields.isEmpty()) {
            out.add(notApplicable(CheckCatalog.A11Y_FORM_LABELS,
                    "No user-entered form fields found" + (skippedFields > 0 ? " (only button/hidden inputs)." : ".")));
        } else if (unnamedFields == 0) {
            out.add(pass(CheckCatalog.A11Y_FORM_LABELS, labelledFields.size() + " form field(s), all with a label, aria-label or title."));
        } else {
            out.add(fail(CheckCatalog.A11Y_FORM_LABELS,
                    unnamedFields + " of " + labelledFields.size() + " form field(s) have no associated label or accessible name."));
        }

        out.addAll(linkAndButtonNameResults(doc));
        out.addAll(iframeResults(doc));
        return out;
    }

    // ------------------------------------------------------------ TECHNICAL

    private List<CheckResult> technicalChecks(PageContext ctx, Document doc) {
        List<CheckResult> out = new ArrayList<>();

        if (ctx.isHttps()) {
            out.add(pass(CheckCatalog.TECH_HTTPS, "The page was served over HTTPS."));
        } else {
            out.add(fail(CheckCatalog.TECH_HTTPS, "The page was served over plain HTTP (" + clip(ctx.finalUrl(), 120) + ")."));
        }

        out.add(pass(CheckCatalog.TECH_RETRIEVABLE,
                "HTTP " + ctx.httpStatus() + " received in " + ctx.durationMs() + " ms, "
                        + ctx.htmlBytes() + " bytes of HTML inspected."));

        boolean hasHtml = doc.selectFirst("html") != null;
        boolean hasHead = doc.selectFirst("head") != null;
        boolean hasBody = doc.selectFirst("body") != null;
        if (hasHtml && hasHead && hasBody) {
            out.add(pass(CheckCatalog.TECH_STRUCTURE, "Document contains <html>, <head> and <body>."));
        } else {
            out.add(fail(CheckCatalog.TECH_STRUCTURE,
                    "Missing structural elements - html:" + hasHtml + ", head:" + hasHead + ", body:" + hasBody + "."));
        }

        Element viewport = doc.selectFirst("meta[name=viewport]");
        if (viewport != null && !isBlank(viewport.attr("content"))) {
            out.add(pass(CheckCatalog.TECH_VIEWPORT, "viewport content=\"" + clip(viewport.attr("content").trim(), 80) + "\"."));
        } else {
            out.add(fail(CheckCatalog.TECH_VIEWPORT, "No <meta name=\"viewport\"> with content found."));
        }

        Element charset = doc.selectFirst("meta[charset]");
        Element httpEquiv = doc.selectFirst("meta[http-equiv=content-type]");
        if (charset != null && !isBlank(charset.attr("charset"))) {
            out.add(pass(CheckCatalog.TECH_CHARSET, "<meta charset=\"" + charset.attr("charset").trim() + "\"> declared."));
        } else if (httpEquiv != null && httpEquiv.attr("content").toLowerCase(Locale.ROOT).contains("charset")) {
            out.add(pass(CheckCatalog.TECH_CHARSET, "Charset declared through <meta http-equiv=\"content-type\">."));
        } else {
            out.add(fail(CheckCatalog.TECH_CHARSET, "No charset declaration found in <head>."));
        }

        out.addAll(linkTargetResults(doc));
        out.addAll(mixedContentResults(ctx, doc));
        return out;
    }

    // -------------------------------------------------------------- CONTENT

    private List<CheckResult> contentChecks(PageContext ctx, Document doc) {
        List<CheckResult> out = new ArrayList<>();

        String text = doc.body() == null ? "" : doc.body().text();
        int words = isBlank(text) ? 0 : text.split("\\s+").length;
        if (words >= MIN_WORDS) {
            out.add(pass(CheckCatalog.CONTENT_TEXT, words + " words of visible text found in <body>."));
        } else {
            out.add(fail(CheckCatalog.CONTENT_TEXT, "Only " + words + " words of visible text found; a useful page usually has " + MIN_WORDS + "+."));
        }

        long headings = doc.select("h1, h2, h3, h4, h5, h6").stream().filter(h -> !isBlank(h.text())).count();
        if (headings >= 2) {
            out.add(pass(CheckCatalog.CONTENT_HEADINGS, headings + " non-empty heading(s) structure the page."));
        } else {
            out.add(fail(CheckCatalog.CONTENT_HEADINGS, headings + " non-empty heading(s) found; the copy is not divided into sections."));
        }

        Elements navs = doc.select("nav");
        long internalLinks = doc.select("a[href]").stream().filter(this::isInternalLink).count();
        if (!navs.isEmpty()) {
            out.add(pass(CheckCatalog.CONTENT_NAV, navs.size() + "<nav> landmark(s) present."));
        } else if (internalLinks >= 3) {
            out.add(pass(CheckCatalog.CONTENT_NAV, "No <nav> element, but " + internalLinks + " internal links can be identified as navigation."));
        } else {
            out.add(fail(CheckCatalog.CONTENT_NAV, "No <nav> landmark and only " + internalLinks + " internal link(s) found."));
        }

        int generic = 0;
        for (Element anchor : doc.select("a[href]")) {
            String label = accessibleName(anchor).toLowerCase(Locale.ROOT).trim();
            if (GENERIC_LINK_TEXTS.contains(label)) {
                generic++;
            }
        }
        if (generic == 0) {
            out.add(pass(CheckCatalog.CONTENT_LINK_TEXT, "No generic link labels such as \"click here\" detected."));
        } else if (generic <= MAX_GENERIC_LINKS) {
            out.add(pass(CheckCatalog.CONTENT_LINK_TEXT, generic + " generic link label(s) found, within the tolerance of " + MAX_GENERIC_LINKS + "."));
        } else {
            out.add(fail(CheckCatalog.CONTENT_LINK_TEXT, generic + " links use generic labels such as \"click here\" or \"read more\"."));
        }
        return out;
    }

    // ------------------------------------------------------------- helpers

    /** Produces the two checks that share one image-alt measurement. */
    private List<CheckResult> imageAltResults(PageContext ctx, Document doc, CheckDefinition seoDef, CheckDefinition a11yDef) {
        List<CheckResult> out = new ArrayList<>();
        Elements images = doc.select("img");
        if (images.isEmpty()) {
            out.add(notApplicable(seoDef, "No <img> elements on this page."));
            out.add(notApplicable(a11yDef, "No <img> elements on this page."));
            return out;
        }
        long missing = images.stream().filter(img -> !img.hasAttr("alt")).count();
        long decorative = images.stream().filter(img -> img.hasAttr("alt") && img.attr("alt").trim().isEmpty()).count();
        long withText = images.size() - missing - decorative;

        String evidence = images.size() + " image(s): " + withText + " with alt text, " + decorative
                + " marked decorative (alt=\"\"), " + missing + " without an alt attribute.";

        if (missing == 0) {
            out.add(pass(seoDef, evidence));
            out.add(pass(a11yDef, evidence));
        } else {
            out.add(fail(seoDef, evidence));
            out.add(fail(a11yDef, evidence));
        }
        return out;
    }

    private List<CheckResult> linkAndButtonNameResults(Document doc) {
        List<CheckResult> out = new ArrayList<>();

        Elements links = doc.select("a[href]");
        long unnamedLinks = links.stream().filter(a -> isBlank(accessibleName(a))).count();
        if (links.isEmpty()) {
            out.add(notApplicable(CheckCatalog.A11Y_LINK_NAMES, "No links found on this page."));
        } else if (unnamedLinks == 0) {
            out.add(pass(CheckCatalog.A11Y_LINK_NAMES, links.size() + " link(s), all with an accessible name."));
        } else {
            out.add(fail(CheckCatalog.A11Y_LINK_NAMES, unnamedLinks + " of " + links.size() + " link(s) have no accessible name."));
        }

        Elements buttons = doc.select("button, input[type=submit], input[type=button], input[type=reset], input[type=image]");
        long unnamedButtons = buttons.stream().filter(b -> isBlank(accessibleName(b))).count();
        if (buttons.isEmpty()) {
            out.add(notApplicable(CheckCatalog.A11Y_BUTTON_NAMES, "No button controls found on this page."));
        } else if (unnamedButtons == 0) {
            out.add(pass(CheckCatalog.A11Y_BUTTON_NAMES, buttons.size() + " button(s), all with an accessible name."));
        } else {
            out.add(fail(CheckCatalog.A11Y_BUTTON_NAMES, unnamedButtons + " of " + buttons.size() + " button(s) have no accessible name."));
        }
        return out;
    }

    private List<CheckResult> iframeResults(Document doc) {
        Elements iframes = doc.select("iframe");
        if (iframes.isEmpty()) {
            return List.of(notApplicable(CheckCatalog.A11Y_IFRAME_TITLE, "No <iframe> elements on this page."));
        }
        long untitled = iframes.stream().filter(f -> isBlank(f.attr("title"))).count();
        if (untitled == 0) {
            return List.of(pass(CheckCatalog.A11Y_IFRAME_TITLE, iframes.size() + "<iframe>(s), all with a title attribute."));
        }
        return List.of(fail(CheckCatalog.A11Y_IFRAME_TITLE, untitled + " of " + iframes.size() + "<iframe>(s) have no title attribute."));
    }

    private List<CheckResult> linkTargetResults(Document doc) {
        Elements anchors = doc.select("a[href]");
        if (anchors.isEmpty()) {
            return List.of(notApplicable(CheckCatalog.TECH_LINK_TARGETS, "No <a> elements found."));
        }
        int empty = 0;
        int javascript = 0;
        int fragmentOnly = 0;
        for (Element anchor : anchors) {
            String href = anchor.attr("href").trim();
            if (href.isEmpty()) {
                empty++;
            } else if (href.toLowerCase(Locale.ROOT).startsWith("javascript:")) {
                javascript++;
            } else if (href.equals("#")) {
                fragmentOnly++;
            }
        }
        int broken = empty + javascript + fragmentOnly;
        if (broken == 0) {
            return List.of(pass(CheckCatalog.TECH_LINK_TARGETS, anchors.size() + " link(s), all with a usable destination."));
        }
        return List.of(fail(CheckCatalog.TECH_LINK_TARGETS,
                broken + " of " + anchors.size() + " link(s) have no usable destination ("
                        + empty + " empty, " + javascript + " javascript:, " + fragmentOnly + " \"#\")."));
    }

    private List<CheckResult> mixedContentResults(PageContext ctx, Document doc) {
        if (!ctx.isHttps()) {
            return List.of(notApplicable(CheckCatalog.TECH_MIXED_CONTENT,
                    "Skipped: the page itself is not served over HTTPS."));
        }
        String[] attributes = {"src", "poster", "href"};
        String[] tags = {"script", "img", "link", "iframe", "source", "video", "audio"};
        int insecure = 0;
        for (String tag : tags) {
            for (Element element : doc.select(tag)) {
                for (String attribute : attributes) {
                    if (!element.hasAttr(attribute)) {
                        continue;
                    }
                    String value = element.attr(attribute).trim().toLowerCase(Locale.ROOT);
                    if (value.startsWith("http://")) {
                        insecure++;
                    }
                }
            }
        }
        if (insecure == 0) {
            return List.of(pass(CheckCatalog.TECH_MIXED_CONTENT, "No http:// subresources referenced from an HTTPS page."));
        }
        return List.of(fail(CheckCatalog.TECH_MIXED_CONTENT, insecure + " insecure http:// subresource reference(s) on an HTTPS page."));
    }

    /** Best-effort accessible name from text, aria, alt and title attributes. */
    private String accessibleName(Element element) {
        if (element.hasAttr("aria-label") && !isBlank(element.attr("aria-label"))) {
            return element.attr("aria-label").trim();
        }
        if (element.hasAttr("aria-labelledby") && !isBlank(element.attr("aria-labelledby"))) {
            StringBuilder builder = new StringBuilder();
            for (String id : element.attr("aria-labelledby").split("\\s+")) {
                Element target = element.ownerDocument() == null ? null : element.ownerDocument().getElementById(id);
                if (target != null) {
                    builder.append(target.text()).append(' ');
                }
            }
            if (!isBlank(builder.toString())) {
                return builder.toString().trim();
            }
        }
        String text = element.text();
        if (!isBlank(text)) {
            return text.trim();
        }
        Elements images = element.select("img[alt]");
        for (Element image : images) {
            if (!isBlank(image.attr("alt"))) {
                return image.attr("alt").trim();
            }
        }
        if (element.hasAttr("value") && !isBlank(element.attr("value"))) {
            return element.attr("value").trim();
        }
        if (!isBlank(element.attr("title"))) {
            return element.attr("title").trim();
        }
        return "";
    }

    private boolean hasAccessibleName(Element field) {
        if (!isBlank(field.attr("aria-label")) || !isBlank(field.attr("aria-labelledby")) || !isBlank(field.attr("title"))) {
            return true;
        }
        String id = field.id();
        if (!isBlank(id)) {
            Element owner = field.ownerDocument().selectFirst("label[for=\"" + escapeSelector(id) + "\"]");
            if (owner != null && !isBlank(owner.text())) {
                return true;
            }
        }
        Element parentLabel = field.closest("label");
        // A placeholder is intentionally not accepted: it disappears once the
        // user types, so it is not a reliable label for screen reader users.
        return parentLabel != null && !isBlank(parentLabel.text());
    }

    private boolean isInternalLink(Element anchor) {
        String href = anchor.attr("href").trim();
        if (href.isEmpty() || href.startsWith("#") || href.toLowerCase(Locale.ROOT).startsWith("javascript:")) {
            return false;
        }
        if (href.startsWith("/") || href.startsWith("./") || href.startsWith("../")) {
            return true;
        }
        if (href.startsWith("?")) {
            return true;
        }
        return !href.startsWith("http://") && !href.startsWith("https://") && !href.startsWith("mailto:")
                && !href.startsWith("tel:");
    }

    private static String escapeSelector(String value) {
        return value.replace("\"", "\\\"");
    }

    private static String textOf(Element element) {
        return element == null ? "" : element.text();
    }

    /** The first meta that issues crawler directives ("robots", "googlebot", "bingbot"). */
    private static Element findRobotsMeta(Document doc) {
        for (Element meta : doc.select("meta[name]")) {
            String name = meta.attr("name").trim().toLowerCase(Locale.ROOT);
            if (name.equals("robots") || name.equals("googlebot") || name.equals("bingbot")) {
                return meta;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max - 1) + "…";
    }

    private CheckResult pass(CheckDefinition def, String evidence) {
        return new CheckResult(def.id(), def.category(), Severity.INFO, CheckStatus.PASS, def.title(),
                evidence, def.whyItMatters(), def.howToFix(), def.codeExample(), def.weight());
    }

    private CheckResult fail(CheckDefinition def, String evidence) {
        return new CheckResult(def.id(), def.category(), def.severity(), CheckStatus.FAIL, def.title(),
                evidence, def.whyItMatters(), def.howToFix(), def.codeExample(), def.weight());
    }

    private CheckResult notApplicable(CheckDefinition def, String evidence) {
        return new CheckResult(def.id(), def.category(), Severity.INFO, CheckStatus.NOT_APPLICABLE, def.title(),
                evidence, def.whyItMatters(), def.howToFix(), def.codeExample(), def.weight());
    }

    private CheckResult notRun(CheckDefinition def, String evidence) {
        return new CheckResult(def.id(), def.category(), Severity.INFO, CheckStatus.NOT_RUN, def.title(),
                evidence, def.whyItMatters(), def.howToFix(), def.codeExample(), def.weight());
    }

    /** Exposed for tests that build a PageContext from a fixture URI. */
    public static PageContext contextFor(URI uri, Document doc, int status, long durationMs, int bytes) {
        return new PageContext(uri, doc, uri.toString(), status, durationMs, bytes);
    }
}
