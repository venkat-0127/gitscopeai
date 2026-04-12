package com.lms.Digital_library.controller;

import com.lms.Digital_library.dto.NewsDto;
import com.lms.Digital_library.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for Newspaper Hub (search, trending, ingest, details, click counter).
 *
 * Base path: /api/newspapers
 *
 * The controller keeps responsibilities small: validate request params and forward to NewsService.
 * Implement the business logic in NewsService (search, trending, upload, findById, incrementClick, delete).
 */
@RestController
@RequestMapping("/api/newspapers")
@CrossOrigin(origins = "*") // ⚠️ tighten in production (use allowed origins)
public class NewsController {

    private final NewsService service;

    public NewsController(NewsService service) {
        this.service = service;
    }

    /**
     * Paginated search / listing endpoint used by the frontend.
     * Example: GET /api/newspapers/list?q=education&pub=The%20Hindu&page=0&size=10&sort=newest
     *
     * Supported query params:
     *  - q     : free-text search against title/summary
     *  - pub   : source/publication filter (exact match)
     *  - year  : numeric year filter (optional)
     *  - type  : type/category filter (optional)
     *  - sort  : "newest" | "oldest" | "relevance" (default newest)
     *  - page  : page index (default 0)
     *  - size  : page size (default 10)
     */
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<NewsDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String pub,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Sort sortObj = switch (sort.toLowerCase()) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "publishedAt");
            case "relevance" -> Sort.by(Sort.Direction.DESC, "score"); // score optionally maintained by trending job
            default -> Sort.by(Sort.Direction.DESC, "publishedAt");
        };

        PageRequest pr = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);
        return service.search(q, pub, year, type, pr);
    }

    /**
     * Return top trending articles (language-neutral or language-specific).
     * Example: GET /api/newspapers/trending?limit=10&lang=en
     */
    @GetMapping(value = "/trending", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<NewsDto> trending(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "en") String lang
    ) {
        return service.trending(limit, lang);
    }

    /**
     * Latest articles across all sources (simple feed).
     * GET /api/newspapers/latest?limit=20
     */
    @GetMapping(value = "/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<NewsDto> latest(@RequestParam(defaultValue = "20") Integer limit) {
        return service.latest(limit);
    }

    /**
     * List articles from a specific source/publication.
     * GET /api/newspapers/source/{sourceName}?limit=50
     */
    @GetMapping(value = "/source/{source}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<NewsDto> bySource(@PathVariable("source") String source,
                                  @RequestParam(defaultValue = "50") Integer limit) {
        return service.bySource(source, limit);
    }

    /**
     * Get single article detail by id.
     * GET /api/newspapers/{id}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NewsDto> getById(@PathVariable("id") Long id) {
        Optional<NewsDto> dto = service.findById(id);
        return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Increment click/view counter for analytics/trending (fire-and-forget from frontend).
     * POST /api/newspapers/click/{id}
     */
    @PostMapping("/click/{id}")
    public ResponseEntity<Void> click(@PathVariable("id") Long id) {
        service.incrementClicks(id);
        return ResponseEntity.accepted().build();
    }

    /**
     * Upload (ingest) a new article (used by admin or user upload flow).
     * Accepts a JSON NewsDto and returns the saved object (with ID).
     *
     * POST /api/newspapers/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NewsDto> uploadNewspaper(@RequestBody NewsDto dto) {
        NewsDto saved = service.upload(dto);
        // return 201 Created with Location
        return ResponseEntity
                .created(URI.create("/api/newspapers/" + saved.getId()))
                .body(saved);
    }

    /**
     * Delete an article (admin-only — ensure service enforces authorization).
     * DELETE /api/newspapers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        boolean ok = service.delete(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
