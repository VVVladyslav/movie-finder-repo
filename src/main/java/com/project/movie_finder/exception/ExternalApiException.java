package com.project.movie_finder.exception;

/**
 ExternalApiException: indicates failures while calling the external TMDB API (HTTP errors, timeouts, parsing).
 Typically thrown by MovieApiClient; mapped by GlobalExceptionHandler to a 5xx (Bad Gateway/Service Unavailable).
 */

public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}