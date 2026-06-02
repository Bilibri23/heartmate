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
}
