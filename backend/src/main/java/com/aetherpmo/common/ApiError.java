package com.aetherpmo.common;

import org.springframework.http.HttpStatus;

public class ApiError extends RuntimeException {

    private final HttpStatus status;

    public ApiError(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiError notFound(String message) {
        return new ApiError(HttpStatus.NOT_FOUND, message);
    }

    public static ApiError badRequest(String message) {
        return new ApiError(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiError unauthorized(String message) {
        return new ApiError(HttpStatus.UNAUTHORIZED, message);
    }
}
