package com.example.booking.exception;

/**
 * Thrown for authorization failures - e.g. a USER attempting to access
 * another user's reservation. Mapped to 403 Forbidden by the global handler.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
