package com.webscorer.exception;

import org.springframework.http.HttpStatus;

/** Any error that should be translated into a specific HTTP status code. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
