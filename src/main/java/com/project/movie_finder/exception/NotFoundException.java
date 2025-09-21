package com.project.movie_finder.exception;

/**
 Thrown when a requested resource (movie, favorite, etc.) is not found; mapped by GlobalExceptionHandler to HTTP 404.
 */

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
