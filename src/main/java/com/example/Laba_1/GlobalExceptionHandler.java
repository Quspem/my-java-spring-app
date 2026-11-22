package com.example.Laba_1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity handleValidation(MethodArgumentNotValidException ex) {
        java.util.List details = new java.util.ArrayList();
        for (var err : ex.getBindingResult().getFieldErrors()) {
            details.add(Map.of("field", err.getField(), "message", String.valueOf(err.getDefaultMessage())));
        }
        return ResponseEntity.status(400).body(Map.of("error", "Validation failed", "details", details));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity handleStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason(), "status", ex.getStatusCode().value()));
    }
}