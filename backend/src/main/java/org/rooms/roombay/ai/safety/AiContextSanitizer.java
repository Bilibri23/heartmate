package org.rooms.roombay.ai.safety;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Strips sensitive fields from text before it reaches the model (Layer 2 of the safety stack).
 *
 * <p>Used on both retrieved RAG chunks and any user-message text we redisplay back as context.
 * Replaces emails, phone numbers, payment references, document URLs, and obvious internal-note
 * markers with stable redaction tokens so the model never sees them but the structure is intact.
 */
@Component
public class AiContextSanitizer {

    private static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );

    private static final Pattern PHONE = Pattern.compile(
            "(?:\\+?\\d{1,3}[\\s-]?)?(?:\\(?\\d{2,4}\\)?[\\s-]?)?\\d{3}[\\s-]?\\d{2,4}[\\s-]?\\d{2,4}"
    );

    private static final Pattern DOC_URL = Pattern.compile(
            "https?://[^\\s]*?(cloudinary|s3|drive\\.google|dropbox|/uploads/|/documents/|/verifications/)[^\\s]*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PAYMENT_REF = Pattern.compile(
            "\\b(?:MTN|Orange|momo|MOMO|TX|TXN|REF|PMT)[-_:#]?[A-Z0-9]{6,}\\b"
    );

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "\\b[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern INTERNAL_NOTE_LINE = Pattern.compile(
            "(?im)^\\s*(internal[- ]note|admin[- ]note|reviewer[- ]note|reject(ion)?[- ]reason)\\s*[:\\-].*$"
    );

    private static final Pattern SECRET_LINE = Pattern.compile(
            "(?im)^.*\\b(password|api[- ]?key|secret|token|bearer|jwt)\\s*[:=].*$"
    );

    /**
     * Returns a model-safe copy of {@code text}. Never throws on null/empty.
     */
    public String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String s = text;
        s = SECRET_LINE.matcher(s).replaceAll("[REDACTED_SECRET]");
        s = INTERNAL_NOTE_LINE.matcher(s).replaceAll("[REDACTED_INTERNAL_NOTE]");
        s = DOC_URL.matcher(s).replaceAll("[REDACTED_DOC_URL]");
        s = PAYMENT_REF.matcher(s).replaceAll("[REDACTED_PAYMENT_REF]");
        s = EMAIL.matcher(s).replaceAll("[REDACTED_EMAIL]");
        s = PHONE.matcher(s).replaceAll("[REDACTED_PHONE]");
        s = UUID_PATTERN.matcher(s).replaceAll("[REDACTED_ID]");
        return s;
    }

    /**
     * True when the original text contains anything we would have redacted.
     * Useful for output-guard checks against the model's reply.
     */
    public boolean containsSensitiveData(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return EMAIL.matcher(text).find()
                || PHONE.matcher(text).find()
                || DOC_URL.matcher(text).find()
                || PAYMENT_REF.matcher(text).find()
                || SECRET_LINE.matcher(text).find()
                || INTERNAL_NOTE_LINE.matcher(text).find();
    }
}
