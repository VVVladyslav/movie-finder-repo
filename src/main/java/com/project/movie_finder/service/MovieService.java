package com.project.movie_finder.service;

import com.project.movie_finder.client.MovieApiClient;
import com.project.movie_finder.dto.MovieDetailsDTO;
import com.project.movie_finder.dto.MovieListItemDTO;
import com.project.movie_finder.dto.PageResponseDTO;
import com.project.movie_finder.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 Service for movie search and details with basic validation and a 60s in-memory TTL cache; delegates remote calls to MovieApiClient.
 Methods:
 search(query, page): validates query/page, serves from cache by (query,page) or fetches and caches PageResponseDTO<MovieListItemDTO>.
 getDetails(id): validates id, serves from cache by id or fetches and caches MovieDetailsDTO.
 isExpired(entry): small helper to check TTL; inner CacheEntry<T> stores value + expiresAt.
 */

@Service
public class MovieService {

    private static final int MIN_QUERY_LEN = 2;
    private static final long CACHE_TTL_MS = 60_000L;
    private final MovieApiClient client;
    private final ConcurrentHashMap<String, CacheEntry<PageResponseDTO<MovieListItemDTO>>> searchCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CacheEntry<MovieDetailsDTO>> detailsCache = new ConcurrentHashMap<>();

    public MovieService(MovieApiClient client) {
        this.client = Objects.requireNonNull(client, "MovieApiClient is required");
    }

    public PageResponseDTO<MovieListItemDTO> search(String query, int page) {
        String q = (query == null) ? "" : query.trim();
        if (!StringUtils.hasText(q) || q.length() < MIN_QUERY_LEN) {
            throw new BadRequestException("Query must contain at least " + MIN_QUERY_LEN + " characters");
        }
        int p = (page < 1) ? 1 : page;

        String cacheKey = q.toLowerCase() + "::" + p;

        CacheEntry<PageResponseDTO<MovieListItemDTO>> cached = searchCache.get(cacheKey);
        if (!isExpired(cached)) {
            return cached.value;
        }

        PageResponseDTO<MovieListItemDTO> result = client.searchMovies(q, p);

        searchCache.put(cacheKey, new CacheEntry<>(result, System.currentTimeMillis() + CACHE_TTL_MS));
        return result;
    }

    public MovieDetailsDTO getDetails(long id) {
        if (id <= 0) {
            throw new BadRequestException("Movie id must be a positive number");
        }

        CacheEntry<MovieDetailsDTO> cached = detailsCache.get(id);
        if (!isExpired(cached)) {
            return cached.value;
        }

        MovieDetailsDTO details = client.getMovieDetails(id);

        detailsCache.put(id, new CacheEntry<>(details, System.currentTimeMillis() + CACHE_TTL_MS));
        return details;
    }

    private static boolean isExpired(CacheEntry<?> entry) {
        return entry == null || entry.expiresAt <= System.currentTimeMillis();
    }

    private static final class CacheEntry<T> {
        final T value;
        final long expiresAt;
        CacheEntry(T value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }
}