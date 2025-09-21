package com.project.movie_finder.controller;

import com.project.movie_finder.dto.MovieDetailsDTO;
import com.project.movie_finder.dto.MovieListItemDTO;
import com.project.movie_finder.dto.PageResponseDTO;
import com.project.movie_finder.exception.GlobalExceptionHandler;
import com.project.movie_finder.exception.NotFoundException;
import com.project.movie_finder.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 Unit tests for MovieController
 Verifies
 GET /api/movies?query=&page= → returns paginated search results from MovieService
 GET /api/movies without query → returns 400 Bad Request
 GET /api/movies/{id} → returns movie details from MovieService
 GET /api/movies/{id} not found → returns 404 Not Found
 */

class MovieControllerTest {

    private MockMvc mockMvc;
    private MovieService movieService;

    @BeforeEach
    void setup() {
        movieService = Mockito.mock(MovieService.class);
        MovieController controller = new MovieController(movieService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void search_ok_returnsPage() throws Exception {
        List<MovieListItemDTO> items = List.of(
                new MovieListItemDTO(10L, "Inception", "2010", "http://img/p1.jpg")
        );
        PageResponseDTO<MovieListItemDTO> page = new PageResponseDTO<>(items, 1, 1);
        when(movieService.search("incep", 1)).thenReturn(page);

        mockMvc.perform(get("/api/movies")
                        .param("query", "incep")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id", is(10)))
                .andExpect(jsonPath("$.items[0].title", is("Inception")))
                .andExpect(jsonPath("$.items[0].year", is("2010")))
                .andExpect(jsonPath("$.items[0].posterUrl", is("http://img/p1.jpg")));

        verify(movieService).search("incep", 1);
    }

    @Test
    void search_missingQuery_returns400() throws Exception {
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void details_ok_returnsDto() throws Exception {
        MovieDetailsDTO dto = new MovieDetailsDTO(
                10L, "Inception", "2010", 148,
                List.of("Science Fiction", "Action"),
                List.of("Leonardo DiCaprio"),
                "Overview...", "http://img/p1.jpg", 8.3
        );
        when(movieService.getDetails(10L)).thenReturn(dto);

        mockMvc.perform(get("/api/movies/{id}", 10))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.title", is("Inception")))
                .andExpect(jsonPath("$.year", is("2010")))
                .andExpect(jsonPath("$.runtime", is(148)))
                .andExpect(jsonPath("$.genres", hasSize(2)))
                .andExpect(jsonPath("$.actors", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.posterUrl", is("http://img/p1.jpg")))
                .andExpect(jsonPath("$.rating", is(8.3)));

        verify(movieService).getDetails(10L);
    }

    @Test
    void details_notFound_returns404() throws Exception {
        when(movieService.getDetails(999L)).thenThrow(new NotFoundException("not found"));

        mockMvc.perform(get("/api/movies/{id}", 999))
                .andExpect(status().isNotFound());
    }
}