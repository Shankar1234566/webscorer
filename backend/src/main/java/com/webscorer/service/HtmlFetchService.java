package com.webscorer.service;

import com.webscorer.config.ScanProperties;
import com.webscorer.exception.ScanRejectedException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Locale;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

/**
 * Downloads a public HTML document with hard limits.
 *
 * <p>Redirects are followed manually (up to {@code webscorer.scan.max-redirects})
 * so that every hop is re-validated by {@link UrlSafetyService}. The body is
 * capped, the response must be HTML, and no script on the page is ever
 * executed - the scanner only ever looks at markup.</p>
 */
@Service
public class HtmlFetchService {

    private static final int CONNECT_TIMEOUT_FALLBACK = 5000;
    private static final int READ_TIMEOUT_FALLBACK = 10000;
    private static final int MAX_REDIRECTS_FALLBACK = 3;
    private static final int MAX_BYTES_FALLBACK = 1_048_576;

    private final UrlSafetyService urlSafety;
    private final ScanProperties properties;

    public HtmlFetchService(UrlSafetyService urlSafety, ScanProperties properties) {
        this.urlSafety = urlSafety;
        this.properties = properties;
    }

    public FetchedPage fetch(URI startUri) {
        long startedAt = System.currentTimeMillis();
        URI current = startUri;

        int maxRedirects = properties.getMaxRedirects() > 0 ? properties.getMaxRedirects() : MAX_REDIRECTS_FALLBACK;
        for (int hop = 0; hop <= maxRedirects; hop++) {
            // Re-validate every destination: a redirect must not reach an internal host.
            URI validated = urlSafety.validate(current.toString());

            // Jsoup applies a single timeout to connect, read and write; use the
            // more generous of the two configured values so slow pages can finish.
            int totalTimeout = Math.max(
                    properties.getConnectTimeoutMs() > 0 ? properties.getConnectTimeoutMs() : CONNECT_TIMEOUT_FALLBACK,
                    properties.getReadTimeoutMs() > 0 ? properties.getReadTimeoutMs() : READ_TIMEOUT_FALLBACK);

            Connection connection = Jsoup.connect(validated.toString())
                    .userAgent(properties.getUserAgent())
                    .timeout(totalTimeout)
                    .maxBodySize(Math.max(1024, properties.getMaxBytes() > 0 ? properties.getMaxBytes() : MAX_BYTES_FALLBACK))
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .sslSocketFactory(trustPublicCerts());

            Connection.Response response;
            try {
                response = connection.execute();
            } catch (IOException e) {
                throw new ScanRejectedException("Could not download the page: " + safeIoMessage(e), e);
            }

            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = response.header("Location");
                if (location == null || location.isBlank()) {
                    throw new ScanRejectedException("The server sent a redirect (HTTP " + status + ") without a destination.");
                }
                if (hop == maxRedirects) {
                    throw new ScanRejectedException("Too many redirects (limit " + maxRedirects + ").");
                }
                current = resolveRedirect(validated, location);
                continue;
            }
            if (status >= 400) {
                throw new ScanRejectedException(remoteErrorMessage(status));
            }

            String contentType = response.contentType();
            if (contentType != null && !contentType.toLowerCase(Locale.ROOT).contains("html")
                    && !contentType.toLowerCase(Locale.ROOT).contains("xml")) {
                throw new ScanRejectedException("That URL returned '" + contentType + "' instead of an HTML document.");
            }

            byte[] body = response.bodyAsBytes();
            if (body == null || body.length == 0) {
                throw new ScanRejectedException("The server returned an empty response body.");
            }

            Document document;
            try {
                document = response.parse();
            } catch (IOException e) {
                throw new ScanRejectedException("The response could not be parsed as HTML.", e);
            }

            long duration = System.currentTimeMillis() - startedAt;
            return new FetchedPage(document, status, response.url() == null ? validated.toString() : response.url().toString(),
                    duration, body.length);
        }
        throw new ScanRejectedException("Too many redirects (limit " + maxRedirects + ").");
    }

    /**
     * Explains a remote HTTP error honestly: the URL passed our own safety
     * checks, so a 4xx here is the *website's* decision, not our SSRF layer.
     * The message is safe to show verbatim - it never echoes server internals.
     */
    static String remoteErrorMessage(int status) {
        return switch (status) {
            case 401, 403 -> "The website refused the request (HTTP " + status + "). "
                    + "The URL passed our safety checks, so this is the site's own access control or "
                    + "bot protection (for example Cloudflare) declining the automated download - "
                    + "our scanner did not block it and no internal address was involved.";
            case 404, 410 -> "The page was not found (HTTP " + status + "). Check the address for typos.";
            case 429 -> "The website is limiting how often it answers automated requests (HTTP 429). "
                    + "Try again in a few minutes.";
            case 500, 502, 503, 504 -> "The website hit a server error (HTTP " + status + ") while "
                    + "answering the request. That is on their side - try again later.";
            default -> "The server responded with HTTP " + status + ". The page could not be retrieved.";
        };
    }

    private URI resolveRedirect(URI base, String location) {
        try {
            URI target = new URI(location.trim());
            URI resolved = target.isAbsolute() ? target : base.resolve(target);
            if (!"http".equalsIgnoreCase(resolved.getScheme()) && !"https".equalsIgnoreCase(resolved.getScheme())) {
                throw new ScanRejectedException("A redirect pointed at a non-HTTP destination.");
            }
            return resolved;
        } catch (URISyntaxException e) {
            throw new ScanRejectedException("The server sent a malformed redirect destination.", e);
        }
    }

    private String safeIoMessage(IOException e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return message.replaceAll("[\\r\\n]+", " ");
    }

    /** The scanner talks to public servers; use the default JVM trust store. */
    private javax.net.ssl.SSLSocketFactory trustPublicCerts() {
        return (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
    }
}
