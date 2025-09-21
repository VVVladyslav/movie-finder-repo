package com.project.movie_finder.exception;

/**
 BadRequestException: thrown for client input/validation errors (HTTP 400).
 Raised by controllers/services on invalid parameters; mapped by GlobalExceptionHandler.
 */

public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}