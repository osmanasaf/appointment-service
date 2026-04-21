package com.asaf.appointment_service.common.exception;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.dto.ApiResponse.ErrorDetails;
import com.asaf.appointment_service.exception.plan.PlanLimitExceededException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleHttpMessageNotReadable_returns400WithUserFriendlyMessage() {
        HttpMessageNotReadableException ex =
            new HttpMessageNotReadableException("invalid json", mock(org.springframework.http.HttpInputMessage.class));

        ResponseEntity<ApiResponse<Void>> response = handler.handleHttpMessageNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorDetails error = response.getBody().error();
        assertThat(error.code()).isEqualTo("INVALID_REQUEST_BODY");
        assertThat(error.message()).isEqualTo("İstek gövdesi okunamadı.");
    }

    @Test
    void handleMissingParameter_returns400WithParameterName() {
        MissingServletRequestParameterException ex =
            new MissingServletRequestParameterException("startDate", "String");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParameter(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorDetails error = response.getBody().error();
        assertThat(error.code()).isEqualTo("MISSING_PARAMETER");
        assertThat(error.message()).contains("startDate");
    }

    @Test
    void handleAccessDenied_returns403WithGenericMessage() {
        AccessDeniedException ex = new AccessDeniedException("denied");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorDetails error = response.getBody().error();
        assertThat(error.code()).isEqualTo("ACCESS_DENIED");
        assertThat(error.message()).isEqualTo("Bu işlem için yetkiniz yok.");
    }

    @Test
    void handleBadCredentials_returns401WithLocalizedMessage() {
        BadCredentialsException ex = new BadCredentialsException("bad creds");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorDetails error = response.getBody().error();
        assertThat(error.code()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(error.message()).isEqualTo("E-posta veya şifre hatalı.");
    }

    @Test
    void handleValidationException_returnsFieldErrorsInDetails() {
        List<ValidationException.FieldError> fieldErrors = List.of(
            new ValidationException.FieldError("email", "E-posta zorunlu"),
            new ValidationException.FieldError("password", "Şifre çok kısa")
        );
        ValidationException ex = new ValidationException(fieldErrors);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorDetails error = response.getBody().error();
        assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(error.details()).isEqualTo(fieldErrors);
    }

    @Test
    void handlePlanLimitExceeded_returns402WithUpgradeCode() {
        PlanLimitExceededException ex = new PlanLimitExceededException("Aktif plan limitiniz doldu.");

        ResponseEntity<ApiResponse<Void>> response = handler.handlePlanLimitExceeded(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        ErrorDetails error = response.getBody().error();
        assertThat(error.code()).isEqualTo("PLAN_LIMIT_EXCEEDED");
        assertThat(error.message()).contains("limit");
    }

    @Test
    void handleConstraintViolation_extractsFieldNameFromPath() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("createUser.arg0.email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("E-posta geçersiz");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorDetails error = response.getBody().error();
        assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
        @SuppressWarnings("unchecked")
        List<ValidationException.FieldError> details =
            (List<ValidationException.FieldError>) error.details();
        assertThat(details).hasSize(1);
        assertThat(details.get(0).field()).isEqualTo("email");
        assertThat(details.get(0).message()).isEqualTo("E-posta geçersiz");
    }

    @Test
    void handleGenericException_returns500WithoutLeakingInternalDetails() {
        Exception ex = new RuntimeException("Database password is xxx (PII)");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorDetails error = response.getBody().error();
        assertThat(error.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(error.message()).isEqualTo("Beklenmeyen bir hata oluştu");
        assertThat(error.message()).doesNotContain("Database password");
        assertThat(error.details()).isNull();
    }
}
