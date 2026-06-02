package org.rooms.roombay;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "roombay.schemaValidation.enabled", matches = "true")
class RoombayApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void flywayCreatesRuntimeEntityTables() {
        List<String> requiredTables = List.of(
                "landlord_verification",
                "saved_searches",
                "saved_search_amenities",
                "listing_views",
                "content_flags"
        );

        for (String tableName : requiredTables) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = current_schema() and table_name = ?",
                    Integer.class,
                    tableName
            );
            assertThat(count)
                    .as("Flyway should create table %s before Hibernate validation", tableName)
                    .isEqualTo(1);
        }
    }
}
