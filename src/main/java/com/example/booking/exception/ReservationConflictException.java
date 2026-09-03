package com.example.booking.exception;

/**
 * Thrown when a new/updated reservation would overlap an existing,
 * non-cancelled reservation for the same resource. Mapped to 409 Conflict.
 */
public class ReservationConflictException extends RuntimeException {
    public ReservationConflictException(String message) {
        super(message);
    }
}
