package com.lms.Digital_library.service;

import com.lms.Digital_library.dto.NewsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

public interface NewsService {

    /**
     * Search / list with pagination. Controller builds the PageRequest.
     */
    Page<NewsDto> search(String q, String pub, Integer year, String type, PageRequest pr);

    /**
     * Top trending (pre-scored) articles for a language.
     */
    List<NewsDto> trending(Integer limit, String lang);

    /**
     * Latest articles across all sources.
     */
    List<NewsDto> latest(Integer limit);

    /**
     * Articles from a specific source.
     */
    List<NewsDto> bySource(String source, Integer limit);

    /**
     * Find single article by id.
     */
    Optional<NewsDto> findById(Long id);

    /**
     * Increment view counter (for trending analytics).
     */
    void incrementClicks(Long id);

    /**
     * Upload/ingest a NewsDto into the Article store.
     * Returns the saved DTO (with id).
     */
    NewsDto upload(NewsDto dto);

    /**
     * Delete an article (return true if deleted).
     */
    boolean delete(Long id);
}
