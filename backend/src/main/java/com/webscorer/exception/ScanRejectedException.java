package com.webscorer.exception;

/** Thrown when a URL is rejected by the SSRF / protocol safety rules. */
public class ScanRejectedException extends RuntimeException {

    public ScanRejectedException(String message) {
        super(message);
    }

    public ScanRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
