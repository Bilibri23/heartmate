package org.rooms.roombay.exception;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.rooms.roombay.security.ProfileCompletionRequiredException;
import org.rooms.roombay.service.AppErrorLogService;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GlobalExceptionHandlerTest {

    private final AppErrorLogService appErrorLogService = mock(AppErrorLogService.class);
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(appErrorLogService);

    @Test
    void dataAccessErrorsReturnPublicSafeMessage() {
        var response = handler.handleDataAccessException(
                new DataAccessResourceFailureException("Could not reach PostgreSQL. Check spring.datasource.url"),
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(
                "We could not complete this request right now. Please try again shortly.");
        assertThat(response.getBody().getMessage())
                .doesNotContain("PostgreSQL", "spring.datasource", "DATABASE_URL");
        verify(appErrorLogService).log(eq("ERROR"), eq("DATABASE"), any(), any(), any());
    }

    @Test
    void persistenceErrorsReturnPublicSafeMessage() {
        var response = handler.handlePersistenceException(
                new PersistenceException("relation refresh_token_sessions does not exist"),
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(
                "We could not complete this request right now. Please try again shortly.");
        assertThat(response.getBody().getMessage())
                .doesNotContain("refresh_token_sessions", "PostgreSQL", "schema");
        verify(appErrorLogService).log(eq("ERROR"), eq("DATABASE"), any(), any(), any());
    }

    @Test
    void profileCompletionErrorsReturnClientSafeForbidden() {
        var response = handler.handleProfileCompletionRequiredException(
                new ProfileCompletionRequiredException("Profile completion requirements are not met for operation: MESSAGE"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Profile Completion Required");
        assertThat(response.getBody().getMessage()).isEqualTo(
                "Please finish the required profile steps before using this feature.");
        assertThat(response.getBody().getMessage()).doesNotContain("operation: MESSAGE");
    }
}
