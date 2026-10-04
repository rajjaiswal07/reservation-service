package com.pytm.reservationservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(
            ApiException ex) {

        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", ex.getStatus().value(),
                "error", ex.getStatus().getReasonPhrase(),
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception ex) {

        return ResponseEntity.internalServerError().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 500,
                "error", "Internal Server Error",
                "message", "An unexpected error occurred"
        ));
    }
}