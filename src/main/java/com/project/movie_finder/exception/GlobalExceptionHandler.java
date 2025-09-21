package com.project.movie_finder.exception;

import com.project.movie_finder.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 Global REST exception handler that converts thrown exceptions into JSON ErrorResponseDTO with proper HTTP status.
 Maps: BadRequest/param/type-mismatch/body-parse → 400; NotFound → 404; MethodNotAllowed → 405;
 ExternalApiException → 502 (Bad Gateway); any other Exception → 500. Logs 4xx as warnings and 5xx as errors.
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            BadRequestException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of(messageOrDefault(ex, "Bad request"), "BAD_REQUEST"));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDTO.of(messageOrDefault(ex, "Not found"), "NOT_FOUND"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponseDTO.of("Method not allowed", "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponseDTO> handleExternalApi(ExternalApiException ex) {
        log.error("External API error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponseDTO.of(messageOrDefault(ex, "External service error"), "EXTERNAL_API_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleAny(Exception ex) {
        log.error("Unhandled error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDTO.of("Internal server error", "INTERNAL_SERVER_ERROR"));
    }

    private static String messageOrDefault(Throwable ex, String fallback) {
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? fallback : msg;
    }
}