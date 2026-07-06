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

    @Test
    void auditLogSchemaAlignmentIsCoveredByFlywayMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V49__align_audit_log_schema.sql"
        )).toLowerCase();

        assertThat(migration).contains("create extension if not exists pgcrypto");
        assertThat(migration).contains(
                "alter column id drop default",
                "alter column id type uuid",
                "add column if not exists user_email",
                "add column if not exists user_role",
                "add column if not exists status",
                "idx_audit_entity"
        );
    }

    @Test
    void aiToolCallLogIsCoveredByFlywayMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V50__ai_tool_call_log.sql"
        )).toLowerCase();

        assertThat(migration).contains("create extension if not exists pgcrypto");
        assertThat(migration).contains(
                "create table if not exists ai_tool_call_log",
                "tool_name varchar",
                "allowed boolean",
                "idx_ai_tool_call_log_tool_created"
        );
    }

    @Test
    void roomPreviewMvpIsCoveredByFlywayMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V51__room_preview_mvp.sql"
        )).toLowerCase();

        assertThat(migration).contains("create extension if not exists pgcrypto");
        assertThat(migration).contains(
                "add column if not exists is_unfurnished",
                "add column if not exists room_preview_enabled",
                "add column if not exists room_preview_photo_url",
                "add column if not exists room_preview_status",
                "create table if not exists room_preview_layout",
                "items_json jsonb",
                "idx_room_preview_layout_user_listing"
        );
    }

    @Test
    void preferencesJsonColumnAlignmentIsCoveredByFlywayMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V52__align_preferences_json_columns.sql"
        )).toLowerCase();

        assertThat(migration).contains(
                "alter table roommate_preferences",
                "alter column preferred_locations type jsonb",
                "alter table listing_preferences",
                "alter column property_types type jsonb",
                "using to_jsonb"
        );
    }

    @Test
    void visitsTableIsCoveredByFlywayMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V54__create_visits_table.sql"
        )).toLowerCase();

        assertThat(migration).contains("create extension if not exists pgcrypto");
        assertThat(migration).contains(
                "create table if not exists visits",
                "tenant_id uuid",
                "landlord_id uuid",
                "requested_datetime timestamp",
                "visit_datetime timestamp",
                "status varchar(40)",
                "idx_visits_status",
                "idx_visits_tenant"
        );
    }

    @Test
    void averageRatingTypeAlignmentIsCoveredByFlywayMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V55__align_average_rating_type.sql"
        )).toLowerCase();

        assertThat(migration).contains(
                "alter table property_listings",
                "average_rating",
                "double precision"
        );
    }

    @Test
    void listingTaxonomyMigrationAddsRentSaleAndSharedFields() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V53__listing_taxonomy_and_shared_accommodation.sql"
        )).toLowerCase();

        assertThat(migration).contains(
                "listing_purpose",
                "stay_type",
                "price_period",
                "furnishing_status",
                "is_shared_accommodation",
                "roommates_wanted",
                "property_type = 'shared_room'"
        );
    }

    @Test
    void realtorTablesAreCoveredByFlywayMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "backend",
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V56__create_realtor_tables.sql"
        )).toLowerCase();

        assertThat(migration).contains("create extension if not exists pgcrypto");
        assertThat(migration).contains(
                "create table if not exists realtor_profiles",
                "user_id uuid not null unique references users(id)",
                "verification_status varchar(40) not null default 'pending'",
                "trust_score integer not null default 0",
                "create table if not exists realtor_verification_documents",
                "realtor_id uuid not null references realtor_profiles(id)",
                "idx_realtor_profiles_status",
                "idx_realtor_docs_realtor"
        );
    }
}
