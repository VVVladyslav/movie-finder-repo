package com.project.movie_finder.service;

import com.project.movie_finder.dto.FavoriteDTO;
import com.project.movie_finder.exception.BadRequestException;
import com.project.movie_finder.util.SessionContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 Service that manages per-session Favorites (no accounts/DB).
 Backed by SessionFavoritesStore keyed by an anonymous session id from SessionContext (set by SessionIdFilter).
 Methods:
 list(): returns all favorites for the current session.
 add(FavoriteDTO): validates input and adds to the current session’s store.
 remove(long): removes a favorite by id for the current session.
 currentSessionId(): helper that fetches/validates the session id (throws if missing).
 */

@Service
public class FavoritesService {

    private final SessionFavoritesStore store;

    public FavoritesService(SessionFavoritesStore store) {
        this.store = Objects.requireNonNull(store, "SessionFavoritesStore is required");
    }

    public List<FavoriteDTO> list() {
        String sessionId = currentSessionId();
        return store.list(sessionId);
    }

    public void add(FavoriteDTO favorite) {
        if (favorite == null) {
            throw new BadRequestException("Favorite body must not be null");
        }
        if (favorite.getId() == null || favorite.getId() <= 0) {
            throw new BadRequestException("Favorite 'id' must be a positive number");
        }
        String sessionId = currentSessionId();
        store.add(sessionId, favorite);
    }

    public void remove(long id) {
        if (id <= 0) {
            throw new BadRequestException("Parameter 'id' must be a positive number");
        }
        String sessionId = currentSessionId();
        store.remove(sessionId, id);
    }

    private static String currentSessionId() {
        String sid = SessionContext.get();
        if (sid == null || sid.isBlank()) {
            throw new IllegalStateException("Session id is missing; ensure SessionIdFilter is configured");
        }
        return sid;
    }
}