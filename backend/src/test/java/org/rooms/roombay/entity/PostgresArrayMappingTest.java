package org.rooms.roombay.entity;

import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresArrayMappingTest {

    @Test
    void profileLanguagesUsesPostgresTextArrayMapping() throws Exception {
        var field = Profile.class.getDeclaredField("languages");

        assertThat(field.getAnnotation(Column.class).columnDefinition()).isEqualTo("TEXT[]");
        assertThat(field.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.ARRAY);
    }

    @Test
    void listingAmenitiesUsesPostgresTextArrayMapping() throws Exception {
        var field = PropertyListing.class.getDeclaredField("amenities");

        assertThat(field.getAnnotation(Column.class).columnDefinition()).isEqualTo("TEXT[]");
        assertThat(field.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.ARRAY);
    }
}
