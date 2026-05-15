package com.codesolutions.pmt.shared.api;

import com.codesolutions.pmt.shared.domain.BusinessException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ApiError> handleBusinessException(BusinessException exception) {
    return ResponseEntity.status(exception.status())
        .body(ApiError.of(exception.status(), exception.getMessage(), List.of()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
    List<String> details =
        exception.getBindingResult().getFieldErrors().stream()
            .map(ApiExceptionHandler::formatFieldError)
            .toList();
    return ResponseEntity.badRequest()
        .body(ApiError.of(HttpStatus.BAD_REQUEST, "La requete est invalide.", details));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
    List<String> details =
        exception.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .toList();
    return ResponseEntity.badRequest()
        .body(ApiError.of(HttpStatus.BAD_REQUEST, "La requete est invalide.", details));
  }

  private static String formatFieldError(FieldError error) {
    return error.getField() + ": " + error.getDefaultMessage();
  }

  public record ApiError(
      Instant timestamp, int status, String error, String message, List<String> details) {
    static ApiError of(HttpStatus status, String message, List<String> details) {
      return new ApiError(
          Instant.now(), status.value(), status.getReasonPhrase(), message, details);
    }
  }
}
