package com.lms.Digital_library.controller;

import com.lms.Digital_library.dto.NewsDto;
import com.lms.Digital_library.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/newspapers")
@CrossOrigin(origins = "*") // ⚠️ Adjust in production for security
public class NewsController {

    private final NewsService service;

    // ✅ Constructor injection for NewsService
    public NewsController(NewsService service) {
        this.service = service;
    }

    /**
     * 🔎 Search & filter newspaper articles
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
        return service.search(q, pub, year, type, sort, page, size);
    }

    /**
     * 📈 Get trending articles
     */
    @GetMapping("/trending")
    public List<NewsDto> trending(@RequestParam(defaultValue = "10") Integer limit) {
        return service.trending(limit);
    }

    /**
     * 📤 Upload a new newspaper article
     * (Used by all users — frontend POST request)
     */
    @PostMapping(
            value = "/upload",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<NewsDto> uploadNewspaper(@RequestBody NewsDto dto) {
        // ✅ Call the service to save the article and return the saved object
        NewsDto saved = service.upload(dto);
        return ResponseEntity.ok(saved);
    }
}
