package org.rooms.roombay;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationCoverageTest {

    @Test
    void missingRuntimeTablesAreCoveredByFlywayMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V45__create_missing_runtime_tables.sql"
        )).toLowerCase();

        assertThat(migration).contains("create extension if not exists pgcrypto");

        List<String> requiredTables = List.of(
                "landlord_verification",
                "saved_searches",
                "saved_search_amenities",
                "listing_views",
                "content_flags"
        );

        for (String tableName : requiredTables) {
            assertThat(migration)
                    .as("V45 should create %s", tableName)
                    .contains("create table if not exists " + tableName);
        }
    }

    @Test
    void adminOpsTablesAreCoveredByFlywayMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V46__admin_ops_center.sql"
        )).toLowerCase();

        assertThat(migration).contains("create extension if not exists pgcrypto");

        List<String> requiredTables = List.of(
                "app_error_log",
                "analytics_event",
                "ai_ingest_run"
        );

        for (String tableName : requiredTables) {
            assertThat(migration)
                    .as("V46 should create %s", tableName)
                    .contains("create table if not exists " + tableName);
        }
    }

    @Test
    void opsV2IndexesAreCoveredByFlywayMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V47__ops_v2_indexes.sql"
        )).toLowerCase();

        assertThat(migration).contains(
                "idx_payments_momo_transaction_status",
                "idx_payments_proof_url_status",
                "idx_analytics_event_ai_no_answer_question"
        );
    }
}
