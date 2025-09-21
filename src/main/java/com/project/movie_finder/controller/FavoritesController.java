package com.project.movie_finder.controller;

import com.project.movie_finder.dto.FavoriteDTO;
import com.project.movie_finder.exception.BadRequestException;
import com.project.movie_finder.service.FavoritesService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 REST controller for per-session Favorites
 Endpoints
 GET /api/favorites → returns the current session's favorites list
 POST /api/favorites → adds a favorite (body: FavoriteDTO {id,title,year,posterUrl}); returns 201 + Location
 DELETE /api/favorites/{id} → removes a favorite by TMDB id; returns 204
 Validation: non-null body, id > 0. Storage is in-memory per session via FavoritesService
 */

@RestController
@RequestMapping(path = "/api/favorites", produces = MediaType.APPLICATION_JSON_VALUE)
public class FavoritesController {

    private final FavoritesService favoritesService;

    public FavoritesController(FavoritesService favoritesService) {
        this.favoritesService = favoritesService;
    }

    @GetMapping
    public List<FavoriteDTO> list() {
        return favoritesService.list();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> add(@RequestBody FavoriteDTO favorite) {
        if (favorite == null) {
            throw new BadRequestException("Request body must not be null");
        }
        if (favorite.getId() == null || favorite.getId() <= 0) {
            throw new BadRequestException("Favorite 'id' must be a positive number");
        }

        favoritesService.add(favorite);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/api/favorites/" + favorite.getId());
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable("id") long id) {
        if (id <= 0) {
            throw new BadRequestException("Path variable 'id' must be a positive number");
        }
        favoritesService.remove(id);
        return ResponseEntity.noContent().build();
    }
}