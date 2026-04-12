package com.lms.Digital_library.service;

import com.lms.Digital_library.dto.NewsDto;
import com.lms.Digital_library.entity.NewsArticle;
import com.lms.Digital_library.repository.NewsRepository;
import com.lms.Digital_library.spec.NewsSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class NewsService {

    private final NewsRepository repo;

    public NewsService(NewsRepository repo) {
        this.repo = repo;
    }

    /** Search with combined filters + sort + paging */
    public Page<NewsDto> search(String q, String pub, Integer year, String type,
                                String sort, int page, int size) {

        Specification<NewsArticle> spec = Specification.allOf(
                NewsSpecs.keyword(q),
                NewsSpecs.publication(pub),
                NewsSpecs.year(year),
                NewsSpecs.type(type)
        );

        Sort s = Sort.by("year").descending();
        if ("oldest".equalsIgnoreCase(sort)) {
            s = Sort.by("year").ascending();
        }

        Page<NewsArticle> pg = repo.findAll(spec, PageRequest.of(page, size, s));
        return pg.map(NewsDto::of);
    }

    /** Top viewed items (prefers recent 14 days; falls back to all-time) */
    public List<NewsDto> trending(Integer limit) {
        List<NewsArticle> list =
                repo.findTop10ByCreatedAtAfterOrderByViewsDesc(LocalDate.now().minusDays(14));
        if (list.isEmpty()) list = repo.findTop10ByOrderByViewsDesc();

        if (limit != null && limit > 0 && limit < list.size()) {
            list = list.subList(0, limit);
        }
        return list.stream().map(NewsDto::of).toList();
    }

    /** Create/save a new article from DTO (used by /upload) */
    @Transactional
    public NewsDto upload(NewsDto dto) {
        NewsArticle a = new NewsArticle();

        // NewsDto is a record → use accessors (no "get" prefix)
        a.setTitle(nullToEmpty(dto.title()));
        a.setPub(nullToEmpty(dto.pub()));
        a.setYear(dto.year() != null ? dto.year() : LocalDate.now().getYear());
        a.setType(nullToEmpty(dto.type()));               // epaper/editorials/clippings/govt/current
        a.setTags(nullToEmpty(dto.tags()));               // comma-separated
        a.setSummary(nullToEmpty(dto.summary()));
        a.setUrl(nullToEmpty(dto.url()));
        // optional fields if present in your record:
        // a.setDownloadUrl(nullToEmpty(dto.downloadUrl()));
        // a.setViews(dto.views() != null ? dto.views() : 0);

        // createdAt & views have defaults in entity
        NewsArticle saved = repo.save(a);
        return NewsDto.of(saved);
    }

    /* ------------ helpers ------------ */
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
