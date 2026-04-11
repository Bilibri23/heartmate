package org.rooms.roombay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Stores Help and Support form submissions when Flyway is off in dev.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupportInquirySchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS support_inquiries (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        user_id UUID NOT NULL,
                        email VARCHAR(255),
                        category VARCHAR(64),
                        subject TEXT NOT NULL,
                        message TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_support_inquiries_created ON support_inquiries(created_at DESC)");
            log.info("[Support] support_inquiries table ready");
        } catch (Exception e) {
            log.warn("[Support] Could not init support_inquiries: {}", e.getMessage());
        }
    }
}
