package com.webscorer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.webscorer.config.ScanProperties;
import com.webscorer.exception.ScanRejectedException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The fetch layer against a small local test server.
 *
 * <p>The URL guard is stubbed to allow <b>only</b> the literal {@code 127.0.0.1}
 * of this test server - every other target, and in particular every redirect
 * destination, goes through the real SSRF rules. Production code is untouched:
 * {@code UrlSafetyService} itself still blocks loopback everywhere.</p>
 */
class HtmlFetchServiceTest {

    private HttpServer server;
    private HtmlFetchService service;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> respond(exchange, 200, "text/html",
                "<html lang=\"en\"><head><title>Test page</title></head><body><p>Hello</p></body></html>"));
        server.createContext("/forbidden", exchange ->
                respond(exchange, 403, "text/html", "<html><body>Access denied</body></html>"));
        server.createContext("/missing", exchange ->
                respond(exchange, 404, "text/html", "<html><body>Not here</body></html>"));
        server.createContext("/redirect-metadata", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://169.254.169.254/latest/meta-data/");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/redirect-localhost", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://localhost:9/internal");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();

        ScanProperties properties = new ScanProperties();
        properties.setConnectTimeoutMs(2000);
        properties.setReadTimeoutMs(2000);
        service = new HtmlFetchService(new TestLoopbackOnlySafety(), properties);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Test
    void fetchesAndParsesAnHtmlDocument() {
        FetchedPage page = service.fetch(uri("/ok"));
        assertThat(page.status()).isEqualTo(200);
        assertThat(page.doc().title()).isEqualTo("Test page");
        assertThat(page.bytes()).isPositive();
    }

    @Test
    void remoteHttp403ProducesAClearSafeMessage() {
        assertThatThrownBy(() -> service.fetch(uri("/forbidden")))
                .isInstanceOf(ScanRejectedException.class)
                .hasMessageContaining("HTTP 403")
                .hasMessageContaining("bot protection")
                .hasMessageContaining("passed our safety checks");
    }

    @Test
    void remoteHttp404ProducesAClearMessage() {
        assertThatThrownBy(() -> service.fetch(uri("/missing")))
                .isInstanceOf(ScanRejectedException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void redirectToInternetMetadataAddressIsRejected() {
        assertThatThrownBy(() -> service.fetch(uri("/redirect-metadata")))
                .isInstanceOf(ScanRejectedException.class)
                .hasMessageContaining("private or internal");
    }

    @Test
    void redirectToLocalhostIsRejected() {
        assertThatThrownBy(() -> service.fetch(uri("/redirect-localhost")))
                .isInstanceOf(ScanRejectedException.class)
                .hasMessageContaining("Internal");
    }

    @Test
    void remoteErrorMessagesAreSpecificButSayNothingInternal() {
        assertThat(HtmlFetchService.remoteErrorMessage(404)).contains("not found");
        assertThat(HtmlFetchService.remoteErrorMessage(429)).contains("HTTP 429");
        assertThat(HtmlFetchService.remoteErrorMessage(503)).contains("server error");
        assertThat(HtmlFetchService.remoteErrorMessage(418)).contains("HTTP 418");
        // Always explains that our own safety layer was not what refused the URL.
        assertThat(HtmlFetchService.remoteErrorMessage(403)).doesNotContain("localhost");
    }

    /**
     * Test-only guard: lets this suite reach its own loopback server while every
     * other target - including redirect destinations - hits the real SSRF rules.
     */
    private static final class TestLoopbackOnlySafety extends UrlSafetyService {
        @Override
        public URI validate(String rawUrl) {
            URI uri = URI.create(rawUrl);
            if ("127.0.0.1".equals(uri.getHost())) {
                return uri;
            }
            return super.validate(rawUrl);
        }
    }
}
