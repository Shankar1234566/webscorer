package com.webscorer.service;

import com.webscorer.exception.ScanRejectedException;
import java.net.IDN;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Server-side URL safety rules (SSRF hardening).
 *
 * <p>Only public HTTP(S) targets are allowed. Every URL - including each
 * redirect destination - must pass through {@link #validate(String)} before a
 * connection is opened:</p>
 * <ul>
 *   <li>scheme must be http or https, no embedded credentials;</li>
 *   <li>hostnames such as localhost, *.local or the cloud metadata host are
 *       rejected before any DNS lookup;</li>
 *   <li>the host is resolved and every resolved address is checked against
 *       loopback, private, link-local, CGNAT, multicast and reserved ranges
 *       (IPv4 and IPv6);</li>
 *   <li>the resolved address set must be public, otherwise the request is
 *       refused.</li>
 * </ul>
 *
 * <p>Known limitation: DNS is validated immediately before connecting, so a
 * hostile authoritative server that re-binds between the check and the
 * connection (DNS rebinding) is not fully prevented here. See docs/security.md.</p>
 */
@Service
public class UrlSafetyService {

    private static final String[] BLOCKED_HOST_SUFFIXES = {
            ".localhost", ".local", ".internal", ".home.arpa", ".localdomain",
            ".metadata.google.internal"
    };

    private static final String[] BLOCKED_HOSTS = {
            "localhost", "localhost.localdomain", "metadata.google.internal",
            "metadata", "instance-data", "0.0.0.0"
    };

    /**
     * @return the normalized, validated URI
     * @throws ScanRejectedException with a message that is safe to show to the user
     */
    public URI validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new ScanRejectedException("Enter a website URL to scan.");
        }
        String candidate = rawUrl.trim();
        if (candidate.length() > 2048) {
            throw new ScanRejectedException("That URL is too long (over 2048 characters).");
        }
        // Users often paste "example.com" without a scheme; only add https://
        // when the value carries no scheme at all, otherwise "file://..." would
        // be silently rewritten instead of rejected.
        boolean hasScheme = candidate.matches("^[a-zA-Z][a-zA-Z0-9+.\\-]*://.*");
        if (!hasScheme) {
            candidate = "https://" + candidate;
        }

        URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException e) {
            throw new ScanRejectedException("That is not a valid URL. Use a full address such as https://example.com.");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new ScanRejectedException("Only http:// and https:// URLs can be scanned.");
        }
        if (uri.getRawUserInfo() != null) {
            throw new ScanRejectedException("URLs containing credentials (user:password@) are not allowed.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new ScanRejectedException("The URL does not contain a host name.");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);

        for (String blocked : BLOCKED_HOSTS) {
            if (normalizedHost.equals(blocked)) {
                throw new ScanRejectedException("Internal addresses such as '" + host + "' cannot be scanned.");
            }
        }
        for (String suffix : BLOCKED_HOST_SUFFIXES) {
            if (normalizedHost.endsWith(suffix)) {
                throw new ScanRejectedException("Internal host names such as '" + host + "' cannot be scanned.");
            }
        }

        InetAddress[] addresses;
        try {
            // IDN.toASCII keeps homoglyph punycode handling predictable.
            String asciiHost = IDN.toASCII(normalizedHost);
            addresses = InetAddress.getAllByName(asciiHost);
        } catch (UnknownHostException e) {
            throw new ScanRejectedException("Could not resolve the host '" + host + "'. Check the address and DNS.", e);
        }
        if (addresses.length == 0) {
            throw new ScanRejectedException("The host '" + host + "' did not resolve to any address.");
        }
        for (InetAddress address : addresses) {
            if (isBlocked(address)) {
                throw new ScanRejectedException("The host '" + host + "' resolves to a private or internal address ("
                        + address.getHostAddress() + "), which cannot be scanned.");
            }
        }
        return uri;
    }

    /** True when the address must never be contacted by the scanner. */
    public boolean isBlocked(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();

        if (bytes.length == 4) {
            return isBlockedIpv4(bytes);
        }
        if (bytes.length == 16) {
            // IPv4-mapped IPv6 (::ffff:a.b.c.d) carries an IPv4 address inside.
            boolean v4Mapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) {
                    v4Mapped = false;
                    break;
                }
            }
            if (v4Mapped && (bytes[10] & 0xff) == 0xff && (bytes[11] & 0xff) == 0xff) {
                return isBlockedIpv4(new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]});
            }
            int b0 = bytes[0] & 0xff;
            // Unique local addresses fc00::/7
            if ((b0 & 0xfe) == 0xfc) {
                return true;
            }
            // Deprecated site-local fec0::/10 (not always covered by isSiteLocalAddress).
            if (b0 == 0xfe && (bytes[1] & 0xc0) == 0xc0) {
                return true;
            }
            // The empty address "::"
            if (address instanceof Inet6Address && ((Inet6Address) address).isAnyLocalAddress()) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockedIpv4(byte[] bytes) {
        int b0 = bytes[0] & 0xff;
        int b1 = bytes[1] & 0xff;
        int b2 = bytes[2] & 0xff;

        if (b0 == 0) {                                   // 0.0.0.0/8 "this network"
            return true;
        }
        if (b0 == 10) {                                  // RFC1918 private
            return true;
        }
        if (b0 == 100 && b1 >= 64 && b1 <= 127) {        // CGNAT 100.64.0.0/10
            return true;
        }
        if (b0 == 127) {                                 // loopback
            return true;
        }
        if (b0 == 169 && b1 == 254) {                    // link-local incl. 169.254.169.254 metadata
            return true;
        }
        if (b0 == 172 && b1 >= 16 && b1 <= 31) {         // RFC1918 private
            return true;
        }
        if (b0 == 192 && b1 == 168) {                    // RFC1918 private
            return true;
        }
        if (b0 == 192 && b1 == 0 && b2 == 0) {           // IETF protocol assignments
            return true;
        }
        if (b0 == 198 && (b1 == 18 || b1 == 19)) {       // benchmarking
            return true;
        }
        if (b0 == 198 && b1 == 51 && b2 == 100) {        // benchmarking
            return true;
        }
        if (b0 == 203 && b1 == 0 && b2 == 113) {         // documentation (TEST-NET-3)
            return true;
        }
        if ((b0 & 0xf0) == 0xf0) {                       // reserved 240.0.0.0/4 + broadcast
            return true;
        }
        return false;
    }
}
