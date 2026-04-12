package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.InstitutionalItem;
import com.lms.Digital_library.repository.InstitutionalItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repository")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RepositoryQueryController {

    private final InstitutionalItemRepository repo;

    // ---------- DTO for frontend ----------
    public record RepoRowDTO(
            Long id,
            String title,
            String summary,
            String type,
            String dept,
            String authors,
            Integer year,
            String tags,
            String fileUrl,
            String thumbnailUrl,
            String provider
    ) {}

    public record RepoPage(
            List<RepoRowDTO> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    // ----------------- SEARCH -----------------
    @GetMapping("/search")
    public ResponseEntity<RepoPage> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, name = "dept") String dept,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) Boolean saved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(normalizeFromDatabase(q, type, dept, year, page, size));
    }

    // ----------------- LIST -----------------
    @GetMapping("/list")
    public ResponseEntity<RepoPage> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, name = "dept") String dept,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) Boolean saved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(normalizeFromDatabase(q, type, dept, year, page, size));
    }

    // ---------- Convert DB Results → Frontend Page ----------
    private RepoPage normalizeFromDatabase(String q, String type, String dept, Integer year, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Use repository query (with filters) — see InstitutionalItemRepository
        Page<InstitutionalItem> items = repo.search(
                q.isBlank() ? null : q,
                parseType(type),
                dept,
                year,
                pageable
        );

        List<RepoRowDTO> rows = items.getContent().stream().map(this::toRepoRow).toList();

        return new RepoPage(
                rows,
                items.getNumber(),
                items.getSize(),
                items.getTotalElements(),
                items.getTotalPages()
        );
    }

    // ---------- Map InstitutionalItem → RepoRowDTO ----------
    private RepoRowDTO toRepoRow(InstitutionalItem i) {
        return new RepoRowDTO(
                i.getId(),
                nz(i.getTitle()),
                nz(i.getSummary()),
                i.getType() != null ? i.getType().name() : "",
                nz(i.getDepartment()),
                nz(i.getAuthors()),
                i.getYear(),
                nz(i.getTags()),
                nz(i.getDownloadUrl()),
                nz(i.getThumbnailUrl()),
                "internal" // provider label
        );
    }

    // ---------- Helper ----------
    private InstitutionalItem.Type parseType(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return InstitutionalItem.Type.valueOf(val.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
