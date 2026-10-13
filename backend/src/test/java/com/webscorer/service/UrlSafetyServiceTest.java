package com.webscorer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.webscorer.exception.ScanRejectedException;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;

class UrlSafetyServiceTest {

    private final UrlSafetyService service = new UrlSafetyService();

    @Test
    void rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> service.validate("file:///C:/windows/win.ini"))
                .isInstanceOf(ScanRejectedException.class)
                .hasMessageContaining("http");
        assertThatThrownBy(() -> service.validate("ftp://example.com/x"))
                .isInstanceOf(ScanRejectedException.class);
        assertThatThrownBy(() -> service.validate(null))
                .isInstanceOf(ScanRejectedException.class);
        assertThatThrownBy(() -> service.validate("   "))
                .isInstanceOf(ScanRejectedException.class);
    }

    @Test
    void rejectsLoopbackAndLocalhost() {
        assertThatThrownBy(() -> service.validate("http://localhost:8080/admin"))
                .isInstanceOf(ScanRejectedException.class)
                .hasMessageContaining("Internal");
        assertThatThrownBy(() -> service.validate("http://127.0.0.1/"))
                .isInstanceOf(ScanRejectedException.class);
        assertThatThrownBy(() -> service.validate("http://[::1]/"))
                .isInstanceOf(ScanRejectedException.class);
        assertThatThrownBy(() -> service.validate("http://sub.localhost/"))
                .isInstanceOf(ScanRejectedException.class);
    }

    @Test
    void rejectsPrivateAndLinkLocalAddresses() {
        assertThatThrownBy(() -> service.validate("http://10.0.0.5/"))
                .isInstanceOf(ScanRejectedException.class);
        assertThatThrownBy(() -> service.validate("http://192.168.1.1/"))
                .isInstanceOf(ScanRejectedException.class);
        assertThatThrownBy(() -> service.validate("http://172.16.0.1/"))
                .isInstanceOf(ScanRejectedException.class);
        assertThatThrownBy(() -> service.validate("http://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(ScanRejectedException.class)
                .hasMessageContaining("private or internal");
        assertThatThrownBy(() -> service.validate("http://100.100.100.200/"))
                .isInstanceOf(ScanRejectedException.class);
        assertThatThrownBy(() -> service.validate("https://[fd00::1]/"))
                .isInstanceOf(ScanRejectedException.class);
    }

    @Test
    void rejectsCredentialBearingUrls() {
        assertThatThrownBy(() -> service.validate("https://user:pass@example.com/"))
                .isInstanceOf(ScanRejectedException.class)
                .hasMessageContaining("credentials");
    }

    @Test
    void rejectsInternalHostSuffixes() {
        assertThatThrownBy(() -> service.validate("https://build.internal/"))
                .isInstanceOf(ScanRejectedException.class);
        assertThatThrownBy(() -> service.validate("https://printer.local/"))
                .isInstanceOf(ScanRejectedException.class);
        assertThatThrownBy(() -> service.validate("https://metadata.google.internal/"))
                .isInstanceOf(ScanRejectedException.class);
    }

    @Test
    void acceptsPublicIpLiteralWithoutDns() {
        assertThatCode(() -> service.validate("https://93.184.216.34/"))
                .doesNotThrowAnyException();
    }

    @Test
    void addsHttpsSchemeWhenTheUserOmitsIt() {
        assertThat(service.validate("example.com/page").getScheme()).isEqualTo("https");
    }

    @Test
    void acceptsPublicHostnamesAndAwkwardButLegalPaths() {
        // Real-world public URL shapes, including a path with "~" (bolt.new style).
        assertThat(service.validate("https://example.com/").getHost()).isEqualTo("example.com");
        assertThat(service.validate("https://bolt.new/~/sb1-yh8oquva").getPath()).isEqualTo("/~/sb1-yh8oquva");
        assertThat(service.validate("https://example.com/search?q=a&page=2").getQuery()).isEqualTo("q=a&page=2");
        assertThat(service.validate("http://example.org:8080/deep/path#frag").getHost()).isEqualTo("example.org");
    }

    @Test
    void rejectsMalformedUrls() {
        String[] malformed = {
                "https://",
                "http://",
                "ht tp://example.com",
                "://example.com",
                "https://exa mple.com",
                "https://?q=1",
                "javascript:alert(1)",
                "data:text/html,<script>alert(1)</script>"
        };
        for (String url : malformed) {
            assertThatThrownBy(() -> service.validate(url))
                    .as("must reject: %s", url)
                    .isInstanceOf(ScanRejectedException.class);
        }
    }

    @Test
    void addressClassifierBlocksInternalRangesOnly() throws Exception {
        assertThat(service.isBlocked(InetAddress.getByName("127.0.0.1"))).isTrue();
        assertThat(service.isBlocked(InetAddress.getByName("10.1.2.3"))).isTrue();
        assertThat(service.isBlocked(InetAddress.getByName("169.254.169.254"))).isTrue();
        assertThat(service.isBlocked(InetAddress.getByName("0.0.0.0"))).isTrue();
        assertThat(service.isBlocked(InetAddress.getByName("::1"))).isTrue();
        assertThat(service.isBlocked(InetAddress.getByName("fe80::1"))).isTrue();
        assertThat(service.isBlocked(InetAddress.getByName("93.184.216.34"))).isFalse();
        assertThat(service.isBlocked(InetAddress.getByName("1.1.1.1"))).isFalse();
        assertThat(service.isBlocked(InetAddress.getByName("2606:4700:4700::1111"))).isFalse();
    }
}
