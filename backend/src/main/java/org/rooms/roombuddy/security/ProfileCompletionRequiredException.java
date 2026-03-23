package org.rooms.roombuddy.security;

public class ProfileCompletionRequiredException extends RuntimeException {
    public ProfileCompletionRequiredException(String message) {
        super(message);
    }
}
