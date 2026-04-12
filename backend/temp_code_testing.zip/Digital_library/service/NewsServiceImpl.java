package com.lms.Digital_library.service;

import com.lms.Digital_library.dto.NewsDto;
import com.lms.Digital_library.entity.Article;
import com.lms.Digital_library.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple NewsService implementation using ArticleRepository.
 *
 * This is intentionally pragmatic — it implements controller-expected methods
 * without adding heavy query machinery. You can replace with Criteria/Specification later.
 */
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final ArticleRepository repo;

    @Override
    public Page<NewsDto> search(String q, String pub, Integer year, String type, PageRequest pr) {
        // Simple approach:
        // - If q present -> search by title/summary
        // - Else if pub present -> filter by source
        // - Optionally filter by year (publishedAt year)
        Page<Article> page;
        if (q != null && !q.isBlank()) {
            page = repo.findByTitleContainingIgnoreCaseOrSummaryContainingIgnoreCase(q, q, pr);
        } else {
            page = repo.findAll(pr);
            // If pub/year/type filters required, we filter in-memory (for small datasets).
            // For production, implement repository queries or Specifications.
        }

        List<Article> filtered = page.getContent().stream()
                .filter(a -> {
                    if (pub != null && !pub.isBlank() && (a.getSource() == null || !a.getSource().equalsIgnoreCase(pub)))
                        return false;
                    if (year != null && a.getPublishedAt() != null) {
                        if (a.getPublishedAt().atZone(java.time.ZoneId.systemDefault()).getYear() != year) return false;
                    }
                    // type isn't filled in Article currently; skip or implement mapping
                    return true;
                })
                .collect(Collectors.toList());

        List<NewsDto> dtos = filtered.stream().map(this::toDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, pr, page.getTotalElements());
    }

    @Override
    public List<NewsDto> trending(Integer limit, String lang) {
        if (lang == null || lang.isBlank()) lang = "en";
        List<Article> list = repo.findTop50ByLanguageOrderByScoreDesc(lang);
        return list.stream()
                .limit(Optional.ofNullable(limit).orElse(10))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<NewsDto> latest(Integer limit) {
        Page<Article> p = repo.findAll(PageRequest.of(0, Optional.ofNullable(limit).orElse(20), Sort.by(Sort.Direction.DESC, "publishedAt")));
        return p.getContent().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<NewsDto> bySource(String source, Integer limit) {
        if (source == null) return Collections.emptyList();
        List<Article> list = repo.findTop100BySourceOrderByPublishedAtDesc(source);
        return list.stream().limit(Optional.ofNullable(limit).orElse(50)).map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public Optional<NewsDto> findById(Long id) {
        return repo.findById(id).map(this::toDto);
    }

    @Override
    @Transactional
    public void incrementClicks(Long id) {
        try {
            repo.incrementViews(id);
        } catch (Exception ignored) {
            // Best-effort: do not fail caller if increment fails
        }
    }

    @Override
    @Transactional
    public NewsDto upload(NewsDto dto) {
        Article a = new Article();
        a.setTitle(dto.getTitle());
        a.setSummary(dto.getSummary());
        a.setLink(dto.getLink());
        a.setSource(dto.getSource());
        a.setLanguage(dto.getLanguage() == null ? "en" : dto.getLanguage());
        a.setPublishedAt(dto.getPublishedAt() == null ? Instant.now() : dto.getPublishedAt());
        a.setFetchedAt(Instant.now());
        a.setScore(dto.getScore() == null ? 0.0 : dto.getScore());
        a.setViews(dto.getViews() == null ? 0L : dto.getViews());

        // GUID: prefer link if available; otherwise generate UUID
        if (dto.getLink() != null && !dto.getLink().isBlank()) {
            try {
                // Use normalized URI as guid (safe)
                a.setGuid(URI.create(dto.getLink()).toString());
            } catch (Exception e) {
                a.setGuid(UUID.randomUUID().toString());
            }
        } else {
            a.setGuid(UUID.randomUUID().toString());
        }

        Article saved = repo.save(a);
        return toDto(saved);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }

    /* ============ Helpers ============ */

    private NewsDto toDto(Article a) {
        NewsDto d = new NewsDto();
        d.setId(a.getId());
        d.setTitle(a.getTitle());
        d.setSummary(a.getSummary());
        d.setLink(a.getLink());
        d.setPublishedAt(a.getPublishedAt());
        d.setSource(a.getSource());
        d.setLanguage(a.getLanguage());
        d.setScore(a.getScore());
        d.setViews(a.getViews());
        d.setThumbnailUrl(null);
        // tags not implemented in Article, so leave empty
        return d;
    }
}
