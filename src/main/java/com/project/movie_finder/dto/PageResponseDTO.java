package com.project.movie_finder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

/**
 PageResponseDTO<T>: generic pagination wrapper for search responses.
 Fields: items (list of T, never null), page (1-based current page), total (non-negative total count).
 Normalizes null/invalid inputs and is serialized to JSON for the client.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponseDTO<T> {

    private List<T> items;
    private int page;
    private int total;

    public PageResponseDTO(List<T> items, int page, int total) {
        this.items = (items != null ? items : Collections.emptyList());
        this.page = (page < 1 ? 1 : page);
        this.total = Math.max(total, 0);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getTotal() {
        return total;
    }

    @Override
    public String toString() {
        return "PageResponseDTO{" +
                "items=" + (items != null ? items.size() : 0) +
                ", page=" + page +
                ", total=" + total +
                '}';
    }
}