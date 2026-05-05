package org.rooms.roombay.ai.safety;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Final-stage check before a model reply reaches the user (Layer 3).
 * Detects accidental PII / secret leakage and forces RoomBay's safe fallback.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiOutputGuard {

    private static final String FALLBACK =
            "I’m not able to share that here. If this is about your own account or application, " +
                    "open the relevant screen in RoomBay or contact support@roombay.com.";

    private static final Pattern UNGROUNDED_CLAIM = Pattern.compile(
            "(?i)(I\\s+have\\s+access\\s+to|admin\\s+notes?\\s+say|the\\s+database\\s+shows)"
    );
    private static final Pattern BYPASS_GUIDANCE = Pattern.compile(
            "(?i)(bypass|evade|get\\s+around|skip\\s+verification|avoid\\s+verification|verification\\s+freelancer|without\\s+verification)"
    );

    private final AiContextSanitizer sanitizer;

    public record GuardResult(String safeText, boolean redacted, String reason) {}

    public GuardResult guard(String modelText) {
        if (modelText == null || modelText.isBlank()) {
            return new GuardResult("", false, "empty");
        }
        if (sanitizer.containsSensitiveData(modelText)) {
            log.warn("[AI] output guard tripped: sensitive content detected");
            return new GuardResult(FALLBACK, true, "sensitive_content");
        }
        if (UNGROUNDED_CLAIM.matcher(modelText).find()) {
            log.warn("[AI] output guard tripped: ungrounded internal-data claim");
            return new GuardResult(FALLBACK, true, "ungrounded_claim");
        }
        if (BYPASS_GUIDANCE.matcher(modelText).find()) {
            log.warn("[AI] output guard tripped: bypass-guidance content detected");
            return new GuardResult(FALLBACK, true, "bypass_guidance");
        }
        return new GuardResult(modelText, false, "ok");
    }

    public String fallback() {
        return FALLBACK;
    }
}
