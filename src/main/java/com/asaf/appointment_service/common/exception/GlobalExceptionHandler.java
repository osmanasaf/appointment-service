package com.asaf.appointment_service.common.exception;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.dto.ApiResponse.ErrorDetails;
import com.asaf.appointment_service.exception.auth.EmailNotVerifiedException;
import com.asaf.appointment_service.exception.auth.InvalidOtpException;
import com.asaf.appointment_service.exception.auth.OtpBlockedException;
import com.asaf.appointment_service.exception.employee.EmployeeOnLeaveException;
import com.asaf.appointment_service.exception.plan.PlanFeatureNotAvailableException;
import com.asaf.appointment_service.exception.plan.PlanLimitExceededException;
import com.asaf.appointment_service.exception.token.ActionTokenExpiredException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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

    @ExceptionHandler({PlanLimitExceededException.class, PlanFeatureNotAvailableException.class})
    public ResponseEntity<ApiResponse<Void>> handlePlanUpgradeRequired(BusinessException ex) {
        log.warn("Plan upgrade required. code={}", ex.getCode());
        return ResponseEntity
            .status(HttpStatus.PAYMENT_REQUIRED)
            .body(ApiResponse.error(new ErrorDetails(ex.getCode(), ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(ActionTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleActionTokenExpired(ActionTokenExpiredException ex) {
        log.warn("Action token expired or already used. code={}", ex.getCode());
        return ResponseEntity
            .status(HttpStatus.GONE)
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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {} {}", ex.getMethod(), ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error(new ErrorDetails(
                "METHOD_NOT_ALLOWED",
                "Bu uç nokta için HTTP yöntemi desteklenmiyor: " + ex.getMethod(),
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(new ErrorDetails("BAD_REQUEST", ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(new ErrorDetails("CONFLICT", ex.getMessage(), null, LocalDateTime.now())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Request body not readable: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(new ErrorDetails("INVALID_REQUEST_BODY", "İstek gövdesi okunamadı.", null, LocalDateTime.now())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        String message = String.format("Zorunlu parametre eksik: %s", ex.getParameterName());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(new ErrorDetails("MISSING_PARAMETER", message, null, LocalDateTime.now())));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Parameter type mismatch: {} for parameter {}", ex.getValue(), ex.getName());
        String message = String.format("Parametre tipi hatalı: %s", ex.getName());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(new ErrorDetails("INVALID_PARAMETER", message, null, LocalDateTime.now())));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ValidationException.FieldError> fieldErrors = ex.getConstraintViolations()
            .stream()
            .map(violation -> new ValidationException.FieldError(
                getFieldName(violation),
                violation.getMessage()
            ))
            .toList();

        log.warn("Constraint violation: {} field(s) failed", fieldErrors.size());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(new ErrorDetails("VALIDATION_ERROR", "Girilen veriler geçersiz", fieldErrors, LocalDateTime.now())));
    }

    private String getFieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(new ErrorDetails("ACCESS_DENIED", "Bu işlem için yetkiniz yok.", null, LocalDateTime.now())));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(new ErrorDetails("DATA_CONFLICT", "Bu kayıt başka bir kayıtla çakışıyor.", null, LocalDateTime.now())));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("File too large: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ApiResponse.error(new ErrorDetails("FILE_TOO_LARGE", "Dosya boyutu çok büyük.", null, LocalDateTime.now())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        String correlationId = java.util.UUID.randomUUID().toString();
        log.error("Unexpected error occurred. Correlation ID: {}", correlationId, ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(new ErrorDetails("INTERNAL_ERROR", "Beklenmeyen bir hata oluştu", null, LocalDateTime.now())));
    }
}
