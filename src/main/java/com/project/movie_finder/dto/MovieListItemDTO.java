package com.project.movie_finder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 MovieListItemDTO: compact movie item returned by GET /api/movies (search).
 Fields: id (TMDB id), title, year, posterUrl. Used by MovieController search response.
 Equality/hashCode are based on id; serialized to JSON by Jackson.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovieListItemDTO {

    private Long id;
    private String title;
    private String year;
    private String posterUrl;

    public MovieListItemDTO(Long id, String title, String year, String posterUrl) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.posterUrl = posterUrl;
    }

    public Long getId() {
        return id;
    }

    public MovieListItemDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public String getYear() {
        return year;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovieListItemDTO)) return false;
        MovieListItemDTO that = (MovieListItemDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MovieListItemDTO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", year='" + year + '\'' +
                ", posterUrl='" + posterUrl + '\'' +
                '}';
    }
}