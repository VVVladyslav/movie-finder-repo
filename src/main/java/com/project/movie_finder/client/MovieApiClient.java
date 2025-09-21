package com.project.movie_finder.client;

import com.project.movie_finder.config.MovieApiProperties;
import com.project.movie_finder.dto.MovieDetailsDTO;
import com.project.movie_finder.dto.MovieListItemDTO;
import com.project.movie_finder.dto.PageResponseDTO;
import com.project.movie_finder.exception.BadRequestException;
import com.project.movie_finder.exception.ExternalApiException;
import com.project.movie_finder.exception.NotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 TMDB client for server-side movie search and details retrieval
 Methods
 searchMovies(String query, int page): calls TMDB /search/movie, validates input (>=2 chars), maps to PageResponseDTO<MovieListItemDTO>
 getMovieDetails(long id): calls TMDB /movie/{id}?append_to_response=credits, maps to MovieDetailsDTO (genres, top-5 cast, rating, poster)
 Implementation notes: uses Spring RestClient with JDK HttpClient (timeout/base URL from MovieApiProperties); builds poster URLs via imageBaseUrl
 Throws: BadRequestException (invalid input), NotFoundException (404), ExternalApiException (other client/server errors or network issues)
 */

@Component
public class MovieApiClient {

    private final MovieApiProperties props;
    private final RestClient rest;

    public MovieApiClient(MovieApiProperties props) {
        this.props = Objects.requireNonNull(props, "MovieApiProperties is required");

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getTimeoutMs()))
                .build();

        this.rest = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public PageResponseDTO<MovieListItemDTO> searchMovies(String query, int page) {
        if (!StringUtils.hasText(query) || query.trim().length() < 2) {
            throw new BadRequestException("Query must contain at least 2 characters");
        }

        final int p = Math.max(1, page);

        try {
            TmdbSearchResponse body = rest.get()
                    .uri(uri -> uri.path("/search/movie")
                            .queryParam("query", query.trim())
                            .queryParam("page", p)
                            .queryParam("include_adult", false)
                            .queryParam("language", "en-US")
                            .queryParam("api_key", props.getKey())
                            .build())
                    .retrieve()
                    .onStatus(s -> s.value() == 404,
                            (req, res) -> { throw new NotFoundException("Movies not found"); })
                    .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                            (req, res) -> { throw new ExternalApiException("TMDB search failed with " + res.getStatusCode()); })
                    .body(TmdbSearchResponse.class);

            if (body == null || body.results == null) {
                return new PageResponseDTO<>(List.of(), p, 0);
            }

            List<MovieListItemDTO> items = new ArrayList<>(body.results.size());
            for (TmdbMovieShort r : body.results) {
                items.add(new MovieListItemDTO(
                        r.id,
                        safeString(r.title),
                        toYear(r.release_date),
                        toPosterUrl(r.poster_path)
                ));
            }

            int pageUsed = (body.page != null ? body.page : p);
            int total = (body.total_results != null ? body.total_results : items.size());
            return new PageResponseDTO<>(items, pageUsed, total);

        } catch (RestClientException ex) {
            throw new ExternalApiException("TMDB search request error: " + ex.getMessage(), ex);
        }
    }

    public MovieDetailsDTO getMovieDetails(long id) {
        try {
            TmdbMovieDetails body = rest.get()
                    .uri(uri -> uri.path("/movie/{id}")
                            .queryParam("append_to_response", "credits")
                            .queryParam("language", "en-US")
                            .queryParam("api_key", props.getKey())
                            .build(id))
                    .retrieve()
                    .onStatus(s -> s.value() == 404,
                            (req, res) -> { throw new NotFoundException("Movie with id=" + id + " not found"); })
                    .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                            (req, res) -> { throw new ExternalApiException("TMDB details failed with " + res.getStatusCode()); })
                    .body(TmdbMovieDetails.class);

            if (body == null) {
                throw new ExternalApiException("Empty response from TMDB");
            }

            List<String> genres = new ArrayList<>();
            if (body.genres != null) {
                for (TmdbGenre g : body.genres) {
                    if (g != null && StringUtils.hasText(g.name)) {
                        genres.add(g.name);
                    }
                }
            }

            List<String> actors = new ArrayList<>();
            if (body.credits != null && body.credits.cast != null) {
                body.credits.cast.stream()
                        .filter(c -> c != null && StringUtils.hasText(c.name))
                        .sorted(Comparator.comparingInt(c -> c.order != null ? c.order : Integer.MAX_VALUE))
                        .limit(5)
                        .forEach(c -> actors.add(c.name));
            }

            return new MovieDetailsDTO(
                    body.id,
                    safeString(body.title),
                    toYear(body.release_date),
                    body.runtime,
                    genres,
                    actors,
                    safeString(body.overview),
                    toPosterUrl(body.poster_path),
                    body.vote_average
            );

        } catch (RestClientException ex) {
            throw new ExternalApiException("TMDB details request error: " + ex.getMessage(), ex);
        }
    }

    private String toPosterUrl(String posterPath) {
        if (!StringUtils.hasText(posterPath)) return null;
        String base = props.getImageBaseUrl();
        if (!StringUtils.hasText(base)) return null;
        return base.endsWith("/") ? (base + ltrimSlash(posterPath)) : (base + "/" + ltrimSlash(posterPath));
    }

    private static String ltrimSlash(String s) {
        if (s == null) return null;
        return s.startsWith("/") ? s.substring(1) : s;
    }

    private static String toYear(String releaseDate) {
        if (!StringUtils.hasText(releaseDate) || releaseDate.length() < 4) return null;
        return releaseDate.substring(0, 4);
    }

    private static String safeString(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    static class TmdbSearchResponse {
        public Integer page;
        public List<TmdbMovieShort> results;
        public Integer total_results;
    }

    static class TmdbMovieShort {
        public Long id;
        public String title;
        public String release_date;
        public String poster_path;
    }

    static class TmdbMovieDetails {
        public Long id;
        public String title;
        public String release_date;
        public Integer runtime;
        public String overview;
        public String poster_path;
        public Double vote_average;
        public List<TmdbGenre> genres;
        public TmdbCredits credits;
    }

    static class TmdbGenre {
        public Integer id;
        public String name;
    }

    static class TmdbCredits {
        public List<TmdbCast> cast;
    }

    static class TmdbCast {
        public String name;
        public Integer order;
    }
}