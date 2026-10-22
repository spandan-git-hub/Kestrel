package com.kestrel.common.core.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        MDC.put("correlationId", "corr-test-123");
    }

    @Test
    void testHandleBadRequestException() {
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleBadRequestException(
            new IllegalArgumentException("Invalid discharge value: -5.0")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo("Invalid discharge value: -5.0");
        assertThat(body.getProperties()).containsEntry("correlationId", "corr-test-123");
        assertThat(body.getProperties()).containsKey("timestamp");
    }

    @Test
    void testHandleNotFoundException() {
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleNotFoundException(
            new NoSuchElementException("Location mumbai-unknown not found")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo("Location mumbai-unknown not found");
        assertThat(body.getProperties()).containsEntry("correlationId", "corr-test-123");
    }
}
