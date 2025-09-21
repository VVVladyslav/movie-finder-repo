package com.project.movie_finder.controller;

import com.project.movie_finder.dto.FavoriteDTO;
import com.project.movie_finder.service.FavoritesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 Unit tests for FavoritesController
 Verifies
 GET /api/favorites → returns list from FavoritesService
 POST /api/favorites → delegates to service and returns 201
 DELETE /api/favorites/{id} → delegates to service and returns 204
 */

@WebMvcTest(FavoritesController.class)
class FavoritesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavoritesService favoritesService;

    @Test
    void list_returnsFavorites() throws Exception {
        List<FavoriteDTO> data = List.of(
                new FavoriteDTO(10L, "Inception", "2010", "http://img/p1.jpg"),
                new FavoriteDTO(11L, "Interstellar", "2014", null)
        );
        when(favoritesService.list()).thenReturn(data);

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].title", is("Inception")))
                .andExpect(jsonPath("$[0].year", is("2010")))
                .andExpect(jsonPath("$[0].posterUrl", is("http://img/p1.jpg")))
                .andExpect(jsonPath("$[1].id", is(11)));
    }

    @Test
    void add_returnsCreated_andDelegatesToService() throws Exception {
        doNothing().when(favoritesService).add(any(FavoriteDTO.class));

        String body = """
            {
              "id": 10,
              "title": "Inception",
              "year": "2010",
              "posterUrl": "http://img/p1.jpg"
            }
            """;

        mockMvc.perform(post("/api/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(favoritesService).add(any(FavoriteDTO.class));
    }

    @Test
    void delete_returnsNoContent_andDelegatesToService() throws Exception {
        doNothing().when(favoritesService).remove(eq(10L));

        mockMvc.perform(delete("/api/favorites/{id}", 10))
                .andExpect(status().isNoContent());

        verify(favoritesService).remove(10L);
    }
}