package org.rooms.roombay.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListingRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createRequiresTitleAndRentAmount() {
        ListingRequest request = ListingRequest.builder()
                .status("PENDING")
                .build();

        var violations = validator.validate(request, ListingRequest.Create.class);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("title", "rentAmount");
    }

    @Test
    void updateAllowsPartialPublishPayload() {
        ListingRequest request = ListingRequest.builder()
                .status("PENDING")
                .build();

        var violations = validator.validate(request, ListingRequest.Update.class);

        assertThat(violations).isEmpty();
    }

    @Test
    void updateStillValidatesProvidedNumericFields() {
        ListingRequest request = ListingRequest.builder()
                .rentAmount(-1)
                .build();

        var violations = validator.validate(request, ListingRequest.Update.class);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("rentAmount");
    }
}
