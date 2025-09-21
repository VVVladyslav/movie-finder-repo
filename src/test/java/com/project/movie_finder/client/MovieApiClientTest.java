package com.project.movie_finder.client;

import com.project.movie_finder.config.MovieApiProperties;
import com.project.movie_finder.dto.MovieDetailsDTO;
import com.project.movie_finder.dto.MovieListItemDTO;
import com.project.movie_finder.dto.PageResponseDTO;
import com.project.movie_finder.exception.BadRequestException;
import com.project.movie_finder.exception.ExternalApiException;
import com.project.movie_finder.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 Unit tests for {@link MovieApiClient}
 Uses an in-memory {@link HttpServer} to stub TMDB endpoints and verifies
 search: happy path mapping, 404 -> NotFoundException, 5xx -> ExternalApiException, validation (<2 chars) -> BadRequestException
 details: happy path mapping, 404 -> NotFoundException, 5xx -> ExternalApiException
 */

class MovieApiClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port;
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private MovieApiClient newClient() {
        MovieApiProperties props = new MovieApiProperties();
        props.setBaseUrl(baseUrl);
        props.setImageBaseUrl("http://img.tmdb.org/t/p/w500");
        props.setKey("TEST_KEY");
        props.setTimeoutMs(2_000);
        return new MovieApiClient(props);
    }

    private static void respond(HttpExchange ex, int status, String bodyJson) throws IOException {
        byte[] bytes = bodyJson.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void searchMovies_ok_mapsResponse() {
        server.createContext("/search/movie", exchange -> {
            String json = """
                {
                  "page": 1,
                  "total_results": 2,
                  "results": [
                    { "id": 10, "title": "Inception",   "release_date": "2010-07-16", "poster_path": "/p1.jpg" },
                    { "id": 11, "title": "Interstellar", "release_date": "2014-11-07", "poster_path": null }
                  ]
                }
                """;
            respond(exchange, 200, json);
        });

        MovieApiClient client = newClient();
        PageResponseDTO<MovieListItemDTO> page = client.searchMovies("incep", 1);

        assertEquals(1, page.getPage());
        assertEquals(2, page.getItems().size());

        MovieListItemDTO first = page.getItems().get(0);
        assertEquals(10L, first.getId());
        assertEquals("Inception", first.getTitle());
        assertEquals("2010", first.getYear());
        assertTrue(first.getPosterUrl().endsWith("/p1.jpg"), "poster url should end with /p1.jpg");

        MovieListItemDTO second = page.getItems().get(1);
        assertNull(second.getPosterUrl(), "poster url must be null when path is null");
    }

    @Test
    void searchMovies_404_throwsNotFound() {
        server.createContext("/search/movie", exchange -> respond(exchange, 404, "{\"status\":34}"));
        MovieApiClient client = newClient();
        assertThrows(NotFoundException.class, () -> client.searchMovies("matrix", 1));
    }

    @Test
    void searchMovies_5xx_throwsExternalApi() {
        server.createContext("/search/movie", exchange -> respond(exchange, 500, "{\"error\":\"oops\"}"));
        MovieApiClient client = newClient();
        assertThrows(ExternalApiException.class, () -> client.searchMovies("matrix", 1));
    }

    @Test
    void searchMovies_validation_throwsBadRequest() {
        MovieApiClient client = newClient();
        assertThrows(BadRequestException.class, () -> client.searchMovies("a", 1),
                "should require at least 2 characters");
    }

    @Test
    void getMovieDetails_ok_mapsResponse() {
        server.createContext("/movie/10", (HttpHandler) exchange -> {
            String json = """
                {
                  "id": 10,
                  "title": "Inception",
                  "release_date": "2010-07-16",
                  "runtime": 148,
                  "overview": "A thief who steals corporate secrets...",
                  "poster_path": "/p1.jpg",
                  "vote_average": 8.3,
                  "genres": [ { "id": 1, "name": "Science Fiction" }, { "id": 2, "name": "Action" } ],
                  "credits": {
                    "cast": [
                      { "name": "Leonardo DiCaprio", "order": 0 },
                      { "name": "Joseph Gordon-Levitt", "order": 1 },
                      { "name": "Elliot Page", "order": 2 }
                    ]
                  }
                }
                """;
            respond(exchange, 200, json);
        });

        MovieApiClient client = newClient();
        MovieDetailsDTO d = client.getMovieDetails(10L);

        assertEquals(10L, d.getId());
        assertEquals("Inception", d.getTitle());
        assertEquals("2010", d.getYear());
        assertEquals(148, d.getRuntime());
        assertTrue(d.getPosterUrl().endsWith("/p1.jpg"));
        assertEquals(2, d.getGenres().size());
        assertTrue(d.getActors().size() >= 2, "actors should be parsed");
        assertEquals(8.3, d.getRating());
        assertNotNull(d.getPlot());
    }

    @Test
    void getMovieDetails_404_throwsNotFound() {
        server.createContext("/movie/9999", exchange -> respond(exchange, 404, "{\"status\":34}"));
        MovieApiClient client = newClient();
        assertThrows(NotFoundException.class, () -> client.getMovieDetails(9999L));
    }

    @Test
    void getMovieDetails_5xx_throwsExternalApi() {
        server.createContext("/movie/10", exchange -> respond(exchange, 502, "{\"error\":\"bad gateway\"}"));
        MovieApiClient client = newClient();
        assertThrows(ExternalApiException.class, () -> client.getMovieDetails(10L));
    }
}