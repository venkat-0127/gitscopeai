package com.lms.Digital_library.controller;

import com.lms.Digital_library.dto.RssItemDto;
import com.lms.Digital_library.service.RssNewsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
// Support BOTH base paths so UI can call /api/news/rss/* or /api/rss/*
@RequestMapping({"/api/news/rss", "/api/rss"})
@CrossOrigin(
        origins = {"", "http://localhost:3000", "http://127.0.0.1:8083"},
        allowCredentials = "true"
)
public class RssNewsController {

    private final RssNewsService service;

    public RssNewsController(RssNewsService service) {
        this.service = service;
    }

    @GetMapping("/sources")
    public Map<String, List<RssNewsService.Source>> sources() {
        return service.sources();
    }

    @GetMapping("/search")
    public List<RssItemDto> search(
            @RequestParam(defaultValue = "") String q,      // make optional
            @RequestParam(required = false) String cat,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return service.search(q, cat, limit);
    }
}
