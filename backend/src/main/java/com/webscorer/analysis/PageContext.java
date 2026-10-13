package com.webscorer.analysis;

import java.net.URI;
import org.jsoup.nodes.Document;

/**
 * Everything the analyzers are allowed to look at.
 * Analyzers never open network connections themselves - the fetch layer does.
 *
 * @param uri       validated request URI
 * @param doc       parsed document, null only if parsing itself failed
 * @param finalUrl  URL after redirects
 * @param httpStatus response status code
 * @param durationMs wall-clock time the fetch took
 * @param htmlBytes size of the downloaded (possibly truncated) body
 */
public record PageContext(
        URI uri,
        Document doc,
        String finalUrl,
        int httpStatus,
        long durationMs,
        int htmlBytes
) {
    public boolean isHttps() {
        return "https".equalsIgnoreCase(uri.getScheme());
    }
}
