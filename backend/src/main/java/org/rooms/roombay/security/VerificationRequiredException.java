package org.rooms.roombay.security;

/**
 * Exception thrown when a user tries to access a resource that requires verification
 * but their account is not verified yet.
 */
public class VerificationRequiredException extends RuntimeException {
    
    private final String userRole;
    private final String requiredVerification;
    
    public VerificationRequiredException(String userRole, String requiredVerification) {
        super(String.format("%s must complete %s verification to access this resource", 
                userRole, requiredVerification));
        this.userRole = userRole;
        this.requiredVerification = requiredVerification;
    }
    
    public String getUserRole() {
        return userRole;
    }
    
    public String getRequiredVerification() {
        return requiredVerification;
    }
}
