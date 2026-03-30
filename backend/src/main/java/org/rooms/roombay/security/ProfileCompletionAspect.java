package org.rooms.roombay.security;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.rooms.roombay.service.ProfileCompletionService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class ProfileCompletionAspect {
    private final ProfileCompletionService profileCompletionService;

    @Before("@annotation(requiresCompletion)")
    public void ensureProfileCompletion(RequiresCompletion requiresCompletion) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (!profileCompletionService.isEligible(userId, requiresCompletion.operation())) {
            throw new ProfileCompletionRequiredException(
                    "Profile completion requirements are not met for operation: " + requiresCompletion.operation()
            );
        }
    }
}
