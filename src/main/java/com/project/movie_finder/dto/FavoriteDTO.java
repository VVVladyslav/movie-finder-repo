package com.project.movie_finder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/**
 FavoriteDTO: a per-session “Favorites” item.
 Fields: id (TMDB id), title, year, posterUrl.
 Used by FavoritesController.
 Includes a no-args constructor and setters for Jackson (POST /api/favorites).
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FavoriteDTO {

    private Long id;
    private String title;
    private String year;
    private String posterUrl;

    public FavoriteDTO(Long id, String title, String year, String posterUrl) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.posterUrl = posterUrl;
    }

    public Long getId() {
        return id;
    }

    public FavoriteDTO setId(Long id) {
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
        if (!(o instanceof FavoriteDTO)) return false;
        FavoriteDTO that = (FavoriteDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FavoriteDTO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", year='" + year + '\'' +
                ", posterUrl='" + posterUrl + '\'' +
                '}';
    }
}