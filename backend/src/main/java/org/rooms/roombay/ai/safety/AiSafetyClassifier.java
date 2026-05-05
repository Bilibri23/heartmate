package org.rooms.roombay.ai.safety;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Lightweight intent classifier for clearly out-of-scope or sensitive requests
 * (Layer 4 — input side). Mirrors the behaviour the finetuned model is taught.
 *
 * <p>Pattern-based on purpose: a single, deterministic, easy-to-eval guardrail
 * that fires before any retrieval or generation. The model is still finetuned to
 * reproduce the same refusal style so behaviour is consistent end-to-end.
 */
@Component
public class AiSafetyClassifier {

    public enum Decision {
        ALLOW,
        REFUSE_PRIVATE_USER_DATA,
        REFUSE_INTERNAL_DATA,
        REFUSE_FRAUD_BYPASS,
        REFUSE_LEGAL_OR_MEDICAL,
        REFUSE_PRIVATE_DOC_REQUEST
    }

    private record Rule(Pattern pattern, Decision decision) {}

    private static final List<Rule> RULES = List.of(
            new Rule(compile(
                    "(give|share|send|tell|provide|reveal)\\s+me\\s+(the\\s+)?(landlord|tenant|user|other\\s+user|someone|owner)['’s]*\\s+(phone|email|whatsapp|number|address|contact)"
            ), Decision.REFUSE_PRIVATE_USER_DATA),
            new Rule(compile(
                    "(give|share|send|tell|provide|reveal)\\s+me\\s+(an?other\\s+user|someone\\s+else)['’s]*\\s+(phone|email|whatsapp|number|address|contact)"
            ), Decision.REFUSE_PRIVATE_USER_DATA),
            new Rule(compile(
                    "(give|share|send|tell|provide|reveal)\\s+me\\s+(an?other\\s+user|someone\\s+else)['’s]*\\s+(phone|email|whatsapp|number|address|contact)"
            ), Decision.REFUSE_PRIVATE_USER_DATA),
            new Rule(compile(
                    "(give|share|send|tell|provide|reveal)\\s+me\\s+(an?other\\s+user|someone\\s+else)['’s]*\\s+(phone|email|whatsapp|number|address|contact)"
            ), Decision.REFUSE_PRIVATE_USER_DATA),
            new Rule(compile(
                    "(another|other|someone\\s+else'?s)\\s+(phone|email|password|account|contact|number|address)"
            ), Decision.REFUSE_PRIVATE_USER_DATA),
            new Rule(compile(
                    "(admin|reviewer|moderator|internal)\\s+(notes?|comments?|review|decision)"
            ), Decision.REFUSE_INTERNAL_DATA),
            new Rule(compile(
                    "(reveal|leak|show)\\s+(database|schema|migration|secret|api[- ]?key|env|prompt)"
            ), Decision.REFUSE_INTERNAL_DATA),
            new Rule(compile(
                    "(bypass|evade|fake|forge|skip|cheat|avoid|get\\s+around|work\\s*around)\\s+(the\\s+)?(verification|kyc|identity|trust|background|checks?)"
            ), Decision.REFUSE_FRAUD_BYPASS),
            new Rule(compile(
                    "(how\\s+to|help\\s+me)\\s+(scam|defraud|trick|launder|hide\\s+money)"
            ), Decision.REFUSE_FRAUD_BYPASS),
            new Rule(compile(
                    "(how\\s+can\\s+i|how\\s+do\\s+i|ways?\\s+to)\\s+(bypass|skip|evade|get\\s+around|avoid)\\s+.*(verification|kyc|identity|checks?)"
            ), Decision.REFUSE_FRAUD_BYPASS),
            new Rule(compile(
                    "(fast|quick|quickly|without\\s+verification)\\s+.*(verification|kyc|identity)"
            ), Decision.REFUSE_FRAUD_BYPASS),
            new Rule(compile(
                    "(send|show|share|download)\\s+.*\\b(verification|id|passport|cni|government[- ]id)\\b.*\\b(document|photo|file|copy)\\b"
            ), Decision.REFUSE_PRIVATE_DOC_REQUEST),
            new Rule(compile(
                    "(?:\\bsue\\b|\\blawsuit\\b|legal\\s+advice|prescribe|diagnose|medical\\s+advice)"
            ), Decision.REFUSE_LEGAL_OR_MEDICAL)
    );

    private static Pattern compile(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    public Decision classify(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Decision.ALLOW;
        }
        String text = userMessage.toLowerCase(Locale.ROOT);
        for (Rule r : RULES) {
            if (r.pattern().matcher(text).find()) {
                return r.decision();
            }
        }
        return Decision.ALLOW;
    }

    /**
     * Returns the canonical RoomBay refusal copy for a non-ALLOW decision.
     */
    public String refusalText(Decision decision) {
        return switch (decision) {
            case REFUSE_PRIVATE_USER_DATA ->
                    "I can’t share another user’s personal details like phone, email, or address. " +
                            "If you need to reach a landlord or tenant on RoomBay, use in-app messaging or contact support@roombay.com.";
            case REFUSE_INTERNAL_DATA ->
                    "I can’t share internal admin notes, review decisions, or platform secrets. " +
                            "If you have a question about your own account or application, I can help with that or direct you to support@roombay.com.";
            case REFUSE_FRAUD_BYPASS ->
                    "I can’t help with bypassing verification or any practice that would mislead other users. " +
                            "If you’re stuck on a verification step, I can walk you through the proper process or connect you to support@roombay.com.";
            case REFUSE_LEGAL_OR_MEDICAL ->
                    "I can’t give legal or medical advice. For RoomBay-specific issues, contact support@roombay.com.";
            case REFUSE_PRIVATE_DOC_REQUEST ->
                    "Verification documents are reviewed only by RoomBay admins and aren’t shared with other users. " +
                            "I can’t fetch them for you. If this concerns your own case, contact support@roombay.com.";
            case ALLOW -> "";
        };
    }
}
