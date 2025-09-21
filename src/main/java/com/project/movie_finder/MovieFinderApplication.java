package com.project.movie_finder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 Spring Boot entry point for Movie Finder.
 Starts the application context, scans @ConfigurationProperties, and serves the REST API and static frontend.
 Run from IDE (green triangle) or via Gradle (bootRun).
 */

@SpringBootApplication
@ConfigurationPropertiesScan
public class MovieFinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieFinderApplication.class, args);
    }
}
