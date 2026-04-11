package org.rooms.roombay.exception;

import lombok.Getter;

/**
 * Thrown when a client exceeds a configured request budget (e.g. AI chat per minute).
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
