package com.project.movie_finder.service;

import com.project.movie_finder.dto.FavoriteDTO;
import com.project.movie_finder.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 In-memory, thread-safe per-session Favorites store (ConcurrentHashMap) with 7-day TTL per session
 and a hard cap of 200 items. Throws BadRequestException for invalid input and IllegalStateException
 if session id is missing.
 Methods:
 list(sessionId): returns favorites sorted by title (asc), then id; refreshes session TTL.
 add(sessionId, favorite): validates id, enforces per-session limit, stores a shallow copy; refreshes TTL.
 remove(sessionId, id): validates id and removes; refreshes TTL.
 getOrInitBucket(sessionId): lazy-creates or rotates an expired bucket.
 Helpers: nullSafeLower(..), copyShallow(..). Inner Bucket holds the map and touch() to extend TTL.
 */

@Component
public class SessionFavoritesStore {

    private static final long SESSION_TTL_MS = 7L * 24 * 60 * 60 * 1000;
    private static final int MAX_FAVORITES_PER_SESSION = 200;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public List<FavoriteDTO> list(String sessionId) {
        Bucket b = getOrInitBucket(sessionId);
        b.touch();
        List<FavoriteDTO> out = new ArrayList<>(b.favorites.values());
        out.sort(Comparator
                .comparing((FavoriteDTO f) -> nullSafeLower(f.getTitle()), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(f -> f.getId(), Comparator.nullsLast(Comparator.naturalOrder())));
        return out;
    }

    public void add(String sessionId, FavoriteDTO favorite) {
        Objects.requireNonNull(favorite, "favorite must not be null");
        if (favorite.getId() == null || favorite.getId() <= 0) {
            throw new BadRequestException("Favorite 'id' must be a positive number");
        }
        Bucket b = getOrInitBucket(sessionId);
        b.touch();

        if (!b.favorites.containsKey(favorite.getId()) && b.favorites.size() >= MAX_FAVORITES_PER_SESSION) {
            throw new BadRequestException("Favorites limit exceeded (" + MAX_FAVORITES_PER_SESSION + ")");
        }

        b.favorites.put(favorite.getId(), copyShallow(favorite));
    }

    public void remove(String sessionId, long id) {
        if (id <= 0) {
            throw new BadRequestException("Parameter 'id' must be a positive number");
        }
        Bucket b = getOrInitBucket(sessionId);
        b.touch();
        b.favorites.remove(id);
    }

    private Bucket getOrInitBucket(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalStateException("Session id is missing");
        }

        return buckets.compute(sessionId, (sid, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null) {
                return new Bucket(now);
            }
            if (existing.expiresAt <= now) {
                return new Bucket(now);
            }
            return existing;
        });
    }

    private static String nullSafeLower(String s) {
        return s == null ? null : s.toLowerCase();
    }

    private static FavoriteDTO copyShallow(FavoriteDTO src) {
        return new FavoriteDTO(src.getId(), src.getTitle(), src.getYear(), src.getPosterUrl());
    }

    private static final class Bucket {
        final ConcurrentHashMap<Long, FavoriteDTO> favorites = new ConcurrentHashMap<>();
        volatile long expiresAt;

        Bucket(long now) {
            this.expiresAt = now + SESSION_TTL_MS;
        }

        void touch() {
            long now = System.currentTimeMillis();
            this.expiresAt = now + SESSION_TTL_MS;
        }
    }
}