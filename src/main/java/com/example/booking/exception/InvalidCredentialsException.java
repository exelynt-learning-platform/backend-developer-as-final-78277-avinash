package com.example.booking.exception;

/** Thrown on failed login (bad username or password). Mapped to 401. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
