package com.example.booking.exception;

/**
 * Thrown for semantic validation failures not covered by bean-validation
 * annotations (e.g. endTime before startTime, bad status enum value).
 * Mapped to 400 Bad Request.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
