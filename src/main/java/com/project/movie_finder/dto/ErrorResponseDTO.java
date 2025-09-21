package com.project.movie_finder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 Immutable DTO for API error responses
 Produced by GlobalExceptionHandler and sent to the client when a request fails. The frontend reads these fields to show user-friendly messages
 Fields
 message  human-readable error description
 code     optional machine-readable code (e.g., "bad_request", "not_found")
 timestamp server time when the error was created
 Note: Ensure Jackson can serialize these fields (add public getters, convert to a Java 'record', or annotate fields with @JsonProperty)
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    private final String message;
    private final String code;
    private final Instant timestamp;

    public ErrorResponseDTO(String message, String code) {
        this.message = message;
        this.code = code;
        this.timestamp = Instant.now();
    }

    public static ErrorResponseDTO of(String message, String code) {
        return new ErrorResponseDTO(message, code);
    }
}