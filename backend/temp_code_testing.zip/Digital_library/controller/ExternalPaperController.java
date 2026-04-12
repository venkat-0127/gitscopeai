package com.lms.Digital_library.controller;

import com.lms.Digital_library.dto.ExternalPaperDTO;
import com.lms.Digital_library.dto.ResearchDTO;
import com.lms.Digital_library.service.ExternalPaperService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ExternalPaperController
 *
 * - Provides search endpoints for external providers (CrossRef, arXiv, Semantic Scholar, IEEE placeholder)
 * - Allows importing an external record into the local Research repository.
 *
 * Note: set downloadPdf=true when calling /import if you want the backend to attempt to download
 * and store the PDF (only recommended for open-access sources like arXiv).
 */
@RestController
@RequestMapping("/api/external")
@CrossOrigin(origins = "*") // tighten in production
public class ExternalPaperController {

    private final ExternalPaperService externalService;

    public ExternalPaperController(ExternalPaperService externalService) {
        this.externalService = externalService;
    }

    @GetMapping("/crossref")
    public ResponseEntity<?> searchCrossref(
            @RequestParam("q") String q,
            @RequestParam(value = "rows", defaultValue = "10") int rows
    ) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body("Query parameter 'q' is required");
        }
        try {
            return ResponseEntity.ok(externalService.searchCrossref(q, rows));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error searching CrossRef");
        }
    }

    @GetMapping("/arxiv")
    public ResponseEntity<?> searchArxiv(
            @RequestParam("q") String q,
            @RequestParam(value = "max", defaultValue = "10") int max
    ) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body("Query parameter 'q' is required");
        }
        try {
            return ResponseEntity.ok(externalService.searchArxiv(q, max));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error searching arXiv");
        }
    }

    @GetMapping("/semantic")
    public ResponseEntity<?> searchSemantic(
            @RequestParam("q") String q,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body("Query parameter 'q' is required");
        }
        try {
            return ResponseEntity.ok(externalService.searchSemantic(q, limit));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error searching Semantic Scholar");
        }
    }

    @GetMapping("/ieee")
    public ResponseEntity<?> searchIeee(
            @RequestParam("q") String q,
            @RequestParam(value = "rows", defaultValue = "10") int rows
    ) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body("Query parameter 'q' is required");
        }
        try {
            return ResponseEntity.ok(externalService.searchIeee(q, rows));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error searching IEEE (or not configured)");
        }
    }

    /**
     * Import external paper metadata into local repository.
     *
     * Example:
     *   POST /api/external/import?downloadPdf=false
     *   Content-Type: application/json
     *   Body: ExternalPaperDTO JSON
     *
     * If downloadPdf=true, controller will request the external pdfUrl (if present) and attempt to store it.
     */
    @PostMapping("/import")
    public ResponseEntity<?> importExternal(
            @RequestBody ExternalPaperDTO dto,
            @RequestParam(value = "downloadPdf", defaultValue = "false") boolean downloadPdf
    ) {
        if (dto == null) {
            return ResponseEntity.badRequest().body("Request body is required");
        }
        if ((dto.getTitle() == null || dto.getTitle().isBlank())
                && (dto.getUrl() == null || dto.getUrl().isBlank())
                && (dto.getPdfUrl() == null || dto.getPdfUrl().isBlank())) {
            return ResponseEntity.badRequest().body("At least one of title/url/pdfUrl must be provided");
        }

        try {
            ResearchDTO saved = externalService.importExternal(dto, downloadPdf);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(iae.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Import failed: " + (ex.getMessage() == null ? "unknown error" : ex.getMessage()));
        }
    }
}
