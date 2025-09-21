package com.project.movie_finder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 TMDB settings bound from application properties (prefix "movie.api"); used by MovieApiClient
 Fields
 baseUrl        TMDB API base (no trailing slash)
 imageBaseUrl   Poster image base (no trailing slash)
 key            API key (server-side only)
 timeoutMs      HTTP connect timeout in ms
 Notes: setters trim trailing slashes; toString() omits the key
 */

@Component
@ConfigurationProperties(prefix = "movie.api")
public class MovieApiProperties {

    private String baseUrl;
    private String imageBaseUrl;
    private String key;
    private int timeoutMs = 3000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    public String getImageBaseUrl() {
        return imageBaseUrl;
    }

    public void setImageBaseUrl(String imageBaseUrl) {
        this.imageBaseUrl = trimTrailingSlash(imageBaseUrl);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    private static String trimTrailingSlash(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    @Override
    public String toString() {
        return "MovieApiProperties{" +
                "baseUrl='" + baseUrl + '\'' +
                ", imageBaseUrl='" + imageBaseUrl + '\'' +
                ", timeoutMs=" + timeoutMs +
                '}';
    }
}