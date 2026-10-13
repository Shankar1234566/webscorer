package com.webscorer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable scanner settings. All values come from application.properties or
 * environment variables - nothing security relevant is hardcoded in the code.
 */
@ConfigurationProperties(prefix = "webscorer.scan")
public class ScanProperties {

    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
    private int maxRedirects = 3;
    private int maxBytes = 1_048_576;
    private String userAgent = "WebScorerAI/1.0";

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxRedirects() {
        return maxRedirects;
    }

    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }

    public int getMaxBytes() {
        return maxBytes;
    }

    public void setMaxBytes(int maxBytes) {
        this.maxBytes = maxBytes;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
