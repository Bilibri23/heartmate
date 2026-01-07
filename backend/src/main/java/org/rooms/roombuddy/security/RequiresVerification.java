package org.rooms.roombuddy.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark endpoints that require user verification.
 * 
 * Usage:
 * @RequiresVerification(role = "STUDENT", verificationType = "STUDENT_ID")
 * @RequiresVerification(role = "LANDLORD", verificationType = "IDENTITY")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresVerification {
    
    /**
     * The user role that requires verification (STUDENT or LANDLORD)
     */
    String role();
    
    /**
     * The type of verification required
     * For STUDENT: "STUDENT_ID"
     * For LANDLORD: "IDENTITY", "BUSINESS", "PROPERTY"
     */
    String verificationType() default "STUDENT_ID";
}
