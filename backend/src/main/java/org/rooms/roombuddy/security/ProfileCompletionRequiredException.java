package org.rooms.roombay.security;

public class ProfileCompletionRequiredException extends RuntimeException {
    public ProfileCompletionRequiredException(String message) {
        super(message);
    }
}
