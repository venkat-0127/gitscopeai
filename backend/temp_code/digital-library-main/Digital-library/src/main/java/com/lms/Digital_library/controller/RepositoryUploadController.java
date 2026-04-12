package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.InstitutionalItem;
import com.lms.Digital_library.service.InstitutionalRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/repository")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RepositoryUploadController {

    private final InstitutionalRepositoryService repositoryService;

    /** ===== Helpers ===== */

    /** quick filename check */
private boolean hasPdfExtension(MultipartFile f) {
    if (f == null) return false;
    String original = f.getOriginalFilename();
    if (original == null) original = "";
    String name = original.toLowerCase();
    return name.endsWith(".pdf");
}

    /** read first 5 bytes and check %PDF- signature */
    private boolean looksLikePdf(MultipartFile f) {
        try (InputStream in = f.getInputStream()) {
            byte[] sig = in.readNBytes(5);
            return sig.length == 5 &&
                    sig[0] == 0x25 && sig[1] == 0x50 && sig[2] == 0x44 && sig[3] == 0x46 && sig[4] == 0x2D;
        } catch (IOException e) {
            return false;
        }
    }

    private ResponseEntity<InstitutionalItem> unsupported() {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
    }

    /** ===== Endpoints ===== */

    /**
     * STUDENTS: Upload project reports / theses. PDF only.
     * Saves both the file and metadata into institutional_items table.
     * Expects: title, summary, authors, dept, year, tags, file.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InstitutionalItem> uploadStudentThesis(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String authors,
            @RequestParam(required = false, name = "dept") String department,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String tags,
            @RequestHeader(value = "X-User-Email", required = false) String addedBy
    ) throws IOException {

        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().build();
        if (!hasPdfExtension(file) || !looksLikePdf(file)) return unsupported();

        InstitutionalItem saved = repositoryService.storeAndCreate(
                file,
                "theses",         // storage folder
                title,
                summary,
                authors,
                department,
                year,
                tags,
                "THESIS",         // DB type
                addedBy
        );

        return ResponseEntity.ok(saved);
    }

    /**
     * ADMIN: Upload other archive documents (syllabi, seminar materials, publications, etc.) — PDF only.
     * Expects: type, title, summary, authors, dept, year, tags, file.
     */
    @PostMapping(value = "/admin-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InstitutionalItem> uploadAdminFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String authors,
            @RequestParam(required = false, name = "dept") String department,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String tags,
            @RequestHeader(value = "X-User-Email", required = false) String addedBy
    ) throws IOException {

        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().build();
        if (!hasPdfExtension(file) || !looksLikePdf(file)) return unsupported();

        // Use the provided type both as DB type and as storage subfolder
        String folder = StringUtils.hasText(type) ? type.trim() : "misc";

        InstitutionalItem saved = repositoryService.storeAndCreate(
                file,
                folder,
                title,
                summary,
                authors,
                department,
                year,
                tags,
                type,
                addedBy
        );

        return ResponseEntity.ok(saved);
    }

    /**
     * UPDATE existing item (metadata and optional file replacement). PDF only when replacing file.
     *
     * If a new file is included, we create a *new* row with the merged metadata + new file.
     * (Simplest approach without in-place file replacement; keeps old record intact.)
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InstitutionalItem> update(
            @PathVariable Long id,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String authors,
            @RequestParam(required = false, name = "dept") String department,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String type,
            @RequestHeader(value = "X-User-Email", required = false) String editedBy
    ) throws IOException {

        // Load current item or 404 if missing
        InstitutionalItem item = repositoryService.getExisting(id);

        // If a new file is given: must be a PDF; then create a NEW record with that file and merged metadata
        if (file != null && !file.isEmpty()) {
            if (!hasPdfExtension(file) || !looksLikePdf(file)) return unsupported();

            String folder = StringUtils.hasText(type)
                    ? type
                    : (item.getType() != null ? item.getType().name() : "misc");

            InstitutionalItem created = repositoryService.storeAndCreate(
                    file,
                    folder,
                    StringUtils.hasText(title) ? title : item.getTitle(),
                    StringUtils.hasText(summary) ? summary : item.getSummary(),
                    StringUtils.hasText(authors) ? authors : item.getAuthors(),
                    StringUtils.hasText(department) ? department : item.getDepartment(),
                    (year != null ? year : item.getYear()),
                    StringUtils.hasText(tags) ? tags : item.getTags(),
                    StringUtils.hasText(type) ? type : (item.getType() != null ? item.getType().name() : null),
                    StringUtils.hasText(editedBy) ? editedBy : item.getAddedBy()
            );
            return ResponseEntity.ok(created);
        }

        // Only metadata update (no new file)
        if (StringUtils.hasText(title)) item.setTitle(title);
        if (StringUtils.hasText(summary)) item.setSummary(summary);
        if (StringUtils.hasText(authors)) item.setAuthors(authors);
        if (StringUtils.hasText(department)) item.setDepartment(department);
        if (year != null) item.setYear(year);
        if (StringUtils.hasText(tags)) item.setTags(tags);
        if (StringUtils.hasText(type)) {
            var t = repositoryService.getType(type);
            if (t != null) item.setType(t);
        }
        if (StringUtils.hasText(editedBy)) item.setAddedBy(editedBy);

        InstitutionalItem saved = repositoryService.save(item);
        return ResponseEntity.ok(saved);
    }

    /** DELETE existing item and its physical file (best effort). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            repositoryService.deleteItem(id);   // deletes file (best-effort) + DB row
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException notFound) {
            return ResponseEntity.notFound().build();
        }
    }

    /** HEAD existence check: 200 if exists, 404 if not. */
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headExists(@PathVariable Long id) {
        try {
            repositoryService.getExisting(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException nf) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Optional JSON existence endpoint used by the frontend. */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Map<String, Boolean>> exists(@PathVariable Long id) {
        boolean ok;
        try {
            repositoryService.getExisting(id);
            ok = true;
        } catch (IllegalArgumentException nf) {
            ok = false;
        }
        return ResponseEntity.ok(Map.of("exists", ok));
    }
}
