package com.kestrel.common.core.error;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_KEY = "correlationId";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed for one or more fields"
        );
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://kestrel.local/errors/validation-failed"));

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("errors", fieldErrors);
        enrichMetadata(problem);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ProblemDetail> handleBadRequestException(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://kestrel.local/errors/bad-request"));
        enrichMetadata(problem);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNotFoundException(NoSuchElementException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage() != null ? ex.getMessage() : "Requested resource was not found"
        );
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://kestrel.local/errors/not-found"));
        enrichMetadata(problem);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            ex.getStatusCode(),
            ex.getReason() != null ? ex.getReason() : ex.getMessage()
        );
        problem.setTitle(HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase());
        problem.setType(URI.create("https://kestrel.local/errors/http-status"));
        enrichMetadata(problem);

        return ResponseEntity.status(ex.getStatusCode()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected internal server error occurred"
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://kestrel.local/errors/internal-error"));
        enrichMetadata(problem);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private void enrichMetadata(ProblemDetail problem) {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        problem.setProperty("timestamp", Instant.now().toString());
    }
}
