package com.lms.Digital_library.controller;

import com.lms.Digital_library.dto.ExternalPaperDTO;
import com.lms.Digital_library.dto.ResearchDTO;
import com.lms.Digital_library.entity.ResearchPaper;
import com.lms.Digital_library.repository.ResearchRepository;
import com.lms.Digital_library.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// 🔥 Add these imports
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/research")
@CrossOrigin(origins = "*") // ⚠️ change in production
@Validated
public class ResearchController {

    private final ResearchRepository repo;
    private final FileStorageService storage;

    public ResearchController(ResearchRepository repo, FileStorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    // 📌 LIST / SEARCH
    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "uploadedAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        Page<ResearchPaper> p;
        if (q == null || q.trim().isEmpty()) {
            p = repo.findAll(pageable);
        } else {
            String q2 = q.trim();
            p = repo.findByTitleContainingIgnoreCaseOrAuthorsContainingIgnoreCaseOrTagsContainingIgnoreCase(
                    q2, q2, q2, pageable
            );
        }
        List<ResearchDTO> content = p.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok().body(new PageResponse(content, p.getTotalElements()));
    }

    // 📌 GET by ID (metadata only)
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable("id") Long id) {
        Optional<ResearchPaper> opt = repo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paper not found");
        }
        ResearchPaper p = opt.get();
        ResearchDTO dto = toDto(p);
        dto.setDownloadUrl("/api/research/download/" + id);
        return ResponseEntity.ok(dto);
    }

    // 📌 DOWNLOAD file or redirect to external URL for metadata-only imports
    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable("id") Long id) {
        ResearchPaper paper = repo.findById(id).orElse(null);
        if (paper == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paper not found");
        }

        // If there's a stored filename, attempt to serve it
        String filename = paper.getFilename();
        if (filename != null && !filename.isBlank()) {
            try {
                Resource resource = storage.loadAsResource(filename);
                String originalName = paper.getOriginalFilename() != null ? paper.getOriginalFilename() : filename;
                String contentType = paper.getContentType() != null ? paper.getContentType() : "application/octet-stream";
                String contentDisposition = "inline; filename=\"" + originalName.replace("\"", "") + "\"";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                        .body(resource);
            } catch (MalformedURLException mue) {
                // File missing or unreadable
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            } catch (IllegalArgumentException iae) {
                // invalid filename / path traversal detected
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid filename");
            } catch (Exception ex) {
                ex.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading file");
            }
        }

        // No local file — if originalFilename is a URL, redirect to it
        String orig = paper.getOriginalFilename();
        if (orig != null && (orig.startsWith("http://") || orig.startsWith("https://"))) {
            try {
                URI target = URI.create(orig);
                // 302 Found (redirect to external source). Client will open/download from source.
                return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid original URL");
            }
        }

        // Nothing to serve
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No file available for this paper");
    }

    // 📌 UPLOAD new paper
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("title") @NotBlank String title,
            @RequestParam(value = "authors", required = false) String authors,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("No file uploaded");
        try {
            String storedName = storage.store(file);
            ResearchPaper p = new ResearchPaper();
            p.setTitle(title);
            p.setAuthors(authors);
            p.setYear(year);
            p.setTags(tags);
            p.setFilename(storedName);
            p.setOriginalFilename(file.getOriginalFilename());
            p.setContentType(file.getContentType());
            p.setSizeBytes(file.getSize());
            p.setUploadedAt(Instant.now());
            p.setUploadedBy(uploadedBy);
            repo.save(p);
            ResearchDTO dto = toDto(p);
            dto.setDownloadUrl("/api/research/download/" + p.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
        }
    }

    // 📌 UPDATE metadata (Edit)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMetadata(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Optional<ResearchPaper> opt = repo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paper not found");
        try {
            ResearchPaper p = opt.get();
            if (payload.containsKey("title")) p.setTitle((String) payload.get("title"));
            if (payload.containsKey("authors")) p.setAuthors((String) payload.get("authors"));
            if (payload.containsKey("year")) {
                Object y = payload.get("year");
                if (y instanceof Number) p.setYear(((Number) y).intValue());
                else if (y instanceof String && !((String) y).isBlank()) p.setYear(Integer.parseInt((String) y));
            }
            if (payload.containsKey("tags")) p.setTags((String) payload.get("tags"));
            repo.save(p);
            ResearchDTO dto = toDto(p);
            dto.setDownloadUrl("/api/research/download/" + id);
            return ResponseEntity.ok(dto);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Update failed");
        }
    }

    // 📌 DELETE paper
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        Optional<ResearchPaper> opt = repo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paper not found");
        ResearchPaper paper = opt.get();
        try {
            if (paper.getFilename() != null && !paper.getFilename().isBlank()) {
                storage.delete(paper.getFilename());
            }
        } catch (Exception ignored) {}
        repo.deleteById(id);
        return ResponseEntity.ok().body("Deleted paper with id " + id);
    }

    // 📌 MY SUBMISSIONS (filter by uploader)
    @GetMapping("/my-submissions")
    public ResponseEntity<?> mySubmissions(
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "uploadedAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        Page<ResearchPaper> p = repo.findByUploadedBy(uploadedBy, pageable);
        List<ResearchDTO> content = p.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok().body(new PageResponse(content, p.getTotalElements()));
    }

    // 📌 IMPORT from external (CrossRef/arXiv)
    // Note: this implementation stores metadata-only records (does NOT download remote PDF).
    // If you want to auto-download remote PDFs, call your ExternalPaperService.importExternal(dto, true)
    // and persist the returned ResearchDTO/ResearchPaper — that requires wiring ExternalPaperService here.
    @PostMapping("/import")
    public ResponseEntity<?> importExternal(@RequestBody ExternalPaperDTO dto,
                                            @RequestParam(value = "downloadPdf", defaultValue = "false") boolean downloadPdf) {
        if (dto == null) return ResponseEntity.badRequest().body("Missing payload");
        try {
            // persist metadata-only record (do NOT set filename to the remote URL)
            ResearchPaper p = new ResearchPaper();
            p.setTitle(dto.getTitle() != null ? dto.getTitle() : "Untitled");
            p.setAuthors(dto.getAuthors());
            p.setYear(dto.getYear());
            p.setTags("external" + (dto.getSource() != null ? ("," + dto.getSource()) : ""));
            p.setOriginalFilename(dto.getPdfUrl() != null ? dto.getPdfUrl() : dto.getUrl());
            p.setUploadedAt(Instant.now());
            p.setUploadedBy("external-import");

            // NOTE: do not write remote URL into filename column (keeps DB semantics clean).
            // If you want to download & store the PDF now, integrate ExternalPaperService and call it.
            repo.save(p);

            ResearchDTO response = toDto(p);
            response.setDownloadUrl("/api/research/download/" + p.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Import failed: " + e.getMessage());
        }
    }

    // ===== Helpers =====
    private Sort.Order parseSort(String sortSpec) {
        try {
            String[] parts = sortSpec.split(",");
            String prop = parts[0];
            Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            return new Sort.Order(dir, prop);
        } catch (Exception e) {
            return new Sort.Order(Sort.Direction.DESC, "uploadedAt");
        }
    }

    private ResearchDTO toDto(ResearchPaper p) {
        ResearchDTO d = new ResearchDTO();
        d.setId(p.getId());
        d.setTitle(p.getTitle());
        d.setAuthors(p.getAuthors());
        d.setYear(p.getYear());
        d.setTags(p.getTags());
        d.setOriginalFilename(p.getOriginalFilename());
        d.setContentType(p.getContentType());
        d.setSizeBytes(p.getSizeBytes());
        d.setUploadedAt(p.getUploadedAt());
        d.setUploadedBy(p.getUploadedBy());
        return d;
    }

    public static class PageResponse {
        public final List<ResearchDTO> content;
        public final long totalElements;
        public PageResponse(List<ResearchDTO> content, long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }
    }
}
