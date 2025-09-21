package com.project.movie_finder.service;

import com.project.movie_finder.client.MovieApiClient;
import com.project.movie_finder.dto.MovieDetailsDTO;
import com.project.movie_finder.dto.MovieListItemDTO;
import com.project.movie_finder.dto.PageResponseDTO;
import com.project.movie_finder.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 Unit tests for MovieService
 Verifies
 search with valid query → returns PageResponseDTO from MovieApiClient
 search caches by query and page → calls client once for same key
 search with invalid query → throws BadRequestException
 getDetails with valid id → returns MovieDetailsDTO from MovieApiClient
 getDetails caches by id → calls client once for same id
 getDetails with invalid id → throws BadRequestException
 */

class MovieServiceTest {

    private MovieApiClient client;
    private MovieService service;

    @BeforeEach
    void setUp() {
        client = mock(MovieApiClient.class);
        service = new MovieService(client);
    }

    @Test
    void search_ok_returnsPage_andUsesClient() {
        String q = "inception";
        int page = 1;
        PageResponseDTO<MovieListItemDTO> stub = new PageResponseDTO<>(
                List.of(new MovieListItemDTO(10L, "Inception", "2010", "http://img/p1.jpg")),
                1,
                1
        );
        when(client.searchMovies(q, page)).thenReturn(stub);

        PageResponseDTO<MovieListItemDTO> res = service.search(q, page);

        assertNotNull(res);
        assertEquals(1, res.getPage());
        assertEquals(1, res.getTotal());
        assertEquals(1, res.getItems().size());
        assertEquals(10L, res.getItems().get(0).getId());
        verify(client, times(1)).searchMovies(q, page);
    }

    @Test
    void search_cachesByQueryAndPage_callsClientOnceForSameKey() {
        String q = "matrix";
        int page = 2;
        PageResponseDTO<MovieListItemDTO> stub = new PageResponseDTO<>(
                List.of(new MovieListItemDTO(11L, "The Matrix", "1999", null)),
                2,
                1
        );
        when(client.searchMovies(q, page)).thenReturn(stub);

        PageResponseDTO<MovieListItemDTO> r1 = service.search(q, page);
        PageResponseDTO<MovieListItemDTO> r2 = service.search(q, page);

        assertSame(r1, r2, "expected same object returned from cache");
        verify(client, times(1)).searchMovies(q, page);

        when(client.searchMovies(q, 3)).thenReturn(new PageResponseDTO<>(List.of(), 3, 0));
        service.search(q, 3);
        verify(client, times(1)).searchMovies(q, 3);
    }

    @Test
    void search_invalidQuery_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.search("a", 1));
        assertThrows(BadRequestException.class, () -> service.search(" ", 1));
        assertThrows(BadRequestException.class, () -> service.search(null, 1));
        verifyNoInteractions(client);
    }

    @Test
    void getDetails_ok_returnsDto_andUsesClient() {
        long id = 10L;
        MovieDetailsDTO dto = new MovieDetailsDTO(
                id, "Inception", "2010", 148,
                List.of("Science Fiction", "Action"),
                List.of("Leonardo DiCaprio"),
                "Overview...", "http://img/p1.jpg", 8.3
        );
        when(client.getMovieDetails(id)).thenReturn(dto);

        MovieDetailsDTO res = service.getDetails(id);

        assertNotNull(res);
        assertEquals(id, res.getId());
        assertEquals("Inception", res.getTitle());
        verify(client, times(1)).getMovieDetails(id);
    }

    @Test
    void getDetails_cachesById_callsClientOnceForSameId() {
        long id = 42L;
        MovieDetailsDTO dto = new MovieDetailsDTO(id, "Test", "2024", 120,
                List.of("Drama"), List.of("Actor"), "Plot", null, 7.0);
        when(client.getMovieDetails(id)).thenReturn(dto);

        MovieDetailsDTO d1 = service.getDetails(id);
        MovieDetailsDTO d2 = service.getDetails(id);

        assertSame(d1, d2, "expected same details object returned from cache");
        verify(client, times(1)).getMovieDetails(id);
    }

    @Test
    void getDetails_invalidId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.getDetails(0));
        assertThrows(BadRequestException.class, () -> service.getDetails(-5));
        verifyNoInteractions(client);
    }
}