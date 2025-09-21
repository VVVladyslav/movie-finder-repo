package com.project.movie_finder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 MovieDetailsDTO: full movie info returned by GET /api/movies/{id}.
 Fields: id, title, year, runtime, genres, actors, plot, posterUrl, rating.
 Used by MovieController → frontend modal; serialized to JSON via Jackson.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovieDetailsDTO {

    private Long id;
    private String title;
    private String year;
    private Integer runtime;
    private List<String> genres;
    private List<String> actors;
    private String plot;
    private String posterUrl;
    private Double rating;

    public MovieDetailsDTO(
            Long id,
            String title,
            String year,
            Integer runtime,
            List<String> genres,
            List<String> actors,
            String plot,
            String posterUrl,
            Double rating
    ) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.runtime = runtime;
        this.genres = genres;
        this.actors = actors;
        this.plot = plot;
        this.posterUrl = posterUrl;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public MovieDetailsDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public String getYear() {
        return year;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public List<String> getGenres() {
        return genres;
    }

    public List<String> getActors() {
        return actors;
    }

    public String getPlot() {
        return plot;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public Double getRating() {
        return rating;
    }

    @Override
    public String toString() {
        return "MovieDetailsDTO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", year='" + year + '\'' +
                ", runtime=" + runtime +
                ", genres=" + genres +
                ", actors=" + actors +
                ", plot='" + plot + '\'' +
                ", posterUrl='" + posterUrl + '\'' +
                ", rating=" + rating +
                '}';
    }
}