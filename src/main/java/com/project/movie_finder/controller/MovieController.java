package com.project.movie_finder.controller;

import com.project.movie_finder.dto.MovieDetailsDTO;
import com.project.movie_finder.dto.MovieListItemDTO;
import com.project.movie_finder.dto.PageResponseDTO;
import com.project.movie_finder.exception.BadRequestException;
import com.project.movie_finder.service.MovieService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 REST controller for movie search and details
 Endpoints
 GET /api/movies?query=&page= → paginated search (query ≥ 2 chars; page ≥ 1, defaults to 1)
 GET /api/movies/{id} → details for a movie by TMDB id (> 0)
 Produces application/json and delegates to MovieService
 On invalid parameters, throws BadRequestException handled by global advice
 */

@RestController
@RequestMapping(path = "/api/movies", produces = MediaType.APPLICATION_JSON_VALUE)
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public PageResponseDTO<MovieListItemDTO> search(
            @RequestParam("query") String query,
            @RequestParam(value = "page", required = false) Integer page
    ) {
        int p = (page == null || page < 1) ? 1 : page;
        String q = (query == null) ? "" : query.trim();
        return movieService.search(q, p);
    }

    @GetMapping("/{id}")
    public MovieDetailsDTO details(@PathVariable("id") long id) {
        if (id <= 0) {
            throw new BadRequestException("Path variable 'id' must be a positive number");
        }
        return movieService.getDetails(id);
    }
}