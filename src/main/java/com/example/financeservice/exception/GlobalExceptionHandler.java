package com.example.financeservice.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        ex.getMessage(),
        LocalDateTime.now()
    );
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(ResourceAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.CONFLICT.value(),
        ex.getMessage(),
        LocalDateTime.now()
    );
    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientFundsException(InsufficientFundsException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        ex.getMessage(),
        LocalDateTime.now()
    );
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InvalidTransactionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidTransactionException(InvalidTransactionException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        ex.getMessage(),
        LocalDateTime.now()
    );
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.FORBIDDEN.value(),
        "Access denied: " + ex.getMessage(),
        LocalDateTime.now()
    );
    return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    ValidationErrorResponse errorResponse = new ValidationErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Validation error",
        LocalDateTime.now(),
        errors
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "An unexpected error occurred: " + ex.getMessage(),
        LocalDateTime.now()
    );
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Data
  @AllArgsConstructor
  public static class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
  }

  @Data
  @AllArgsConstructor
  public static class ValidationErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> errors;
  }
}