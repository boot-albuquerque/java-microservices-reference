package com.example.payment.presentation.exception;

import com.example.payment.application.exception.IdempotencyConflictException;
import com.example.payment.application.exception.RateLimitExceededException;
import com.example.payment.domain.exception.InvalidStateTransitionException;
import com.example.payment.domain.exception.PaymentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
    return ResponseEntity.badRequest()
        .body(
            ErrorResponse.of(400, "Bad Request", "Missing required header: " + ex.getHeaderName()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");
    return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Bad Request", message));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
    Throwable cause = ex.getMostSpecificCause();
    String detail = cause != null ? cause.getMessage() : ex.getMessage();
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(400, "Bad Request", "Malformed request body: " + detail));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
  }

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
  }

  @ExceptionHandler(InvalidStateTransitionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidStateTransition(
      InvalidStateTransitionException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
  }

  @ExceptionHandler(IdempotencyConflictException.class)
  public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(ErrorResponse.of(429, "Too Many Requests", ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    log.error("Unhandled exception [{}] in controller layer", ex.getClass().getName(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred"));
  }
}
