package com.asaf.appointment_service.common.exception;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.dto.ApiResponse.ErrorDetails;
import com.asaf.appointment_service.exception.auth.EmailNotVerifiedException;
import com.asaf.appointment_service.exception.auth.InvalidOtpException;
import com.asaf.appointment_service.exception.auth.OtpBlockedException;
import com.asaf.appointment_service.exception.employee.EmployeeOnLeaveException;
import com.asaf.appointment_service.exception.plan.PlanLimitExceededException;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        log.warn("Email not verified: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(new ErrorDetails(ex.getCode(), ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOtp(InvalidOtpException ex) {
        log.warn("Invalid OTP: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(new ErrorDetails("INVALID_OTP", ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(OtpBlockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtpBlocked(OtpBlockedException ex) {
        log.warn("OTP blocked: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiResponse.error(new ErrorDetails("OTP_BLOCKED", ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(new ErrorDetails(ex.getCode(), ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        Object details = ex.getFieldErrors().isEmpty() ? null : ex.getFieldErrors();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(new ErrorDetails(ex.getCode(), ex.getMessage(), details, LocalDateTime.now())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ValidationException.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ValidationException.FieldError(error.getField(), error.getDefaultMessage()))
            .toList();
        
        log.warn("Validation error: {} field(s) failed", fieldErrors.size());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(new ErrorDetails("VALIDATION_ERROR", "Girilen veriler geçersiz", fieldErrors, LocalDateTime.now())));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business error: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.error(new ErrorDetails(ex.getCode(), ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlanLimitExceeded(PlanLimitExceededException ex) {
        log.warn("Plan limit exceeded. code={}", ex.getCode());
        return ResponseEntity
            .status(HttpStatus.PAYMENT_REQUIRED)
            .body(ApiResponse.error(new ErrorDetails(ex.getCode(), ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(EmployeeOnLeaveException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmployeeOnLeave(EmployeeOnLeaveException ex) {
        log.warn("Employee on leave conflict: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(new ErrorDetails(ex.getCode(), ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized request: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(new ErrorDetails(ex.getCode(), ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Login failed: invalid credentials");
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(new ErrorDetails(
                "INVALID_CREDENTIALS",
                "E-posta veya şifre hatalı.",
                null,
                LocalDateTime.now())));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledAccount(DisabledException ex) {
        log.warn("Login failed: account disabled");
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(new ErrorDetails(
                "ACCOUNT_DISABLED",
                "Hesabınız aktif değil.",
                null,
                LocalDateTime.now())));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getResourcePath());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(new ErrorDetails("NOT_FOUND", "İstenen kaynak bulunamadı: " + ex.getResourcePath(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(new ErrorDetails("INTERNAL_ERROR", "Beklenmeyen bir hata oluştu", null, LocalDateTime.now())));
    }
}
