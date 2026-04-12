package com.lms.Digital_library.service;

import com.lms.Digital_library.entity.InstitutionalItem;
import com.lms.Digital_library.entity.InstitutionalItem.Type;
import com.lms.Digital_library.repository.InstitutionalItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class InstitutionalRepositoryService {

    private final InstitutionalItemRepository repo;
    private final FileStorageService fileStorageService;

    /** Search/paginate repository items. */
    public Page<InstitutionalItem> find(String q, String type, String dept, Integer year, int page, int size) {
        Type t = parseType(type);
        return repo.search(q, t, dept, year, PageRequest.of(page, size));
    }

    /** Create a row without file (manual). */
    public InstitutionalItem createManual(InstitutionalItem item) {
        if (item.getType() == null) item.setType(Type.OTHER);
        return repo.save(item);
    }

    /**
     * Store file + create row (used by both student & admin uploads).
     * Now enforces PDF-only. Any non-PDF file will be rejected with IOException.
     */
    public InstitutionalItem storeAndCreate(MultipartFile file,
                                            String folder,
                                            String title,
                                            String summary,
                                            String authors,
                                            String department,
                                            Integer year,
                                            String tags,
                                            String type,
                                            String addedBy) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IOException("No file uploaded.");
        }
        // ---- PDF-only guard ----
        if (!isPdf(file)) {
            throw new IOException("Only PDF files are allowed.");
        }

        FileStorageService.StoredFile sf = fileStorageService.store(file, folder);

        String finalTitle = StringUtils.hasText(title)
                ? title
                : (sf.originalFilename() != null ? sf.originalFilename() : "Untitled");

        Type finalType = parseType(StringUtils.hasText(type) ? type : folder);
        if (finalType == null) finalType = Type.OTHER;

        // We store PDFs only, so public URL directly points to stored file
        String publicUrl = buildPublicUrl(sf.storedFilename(), folder);

        InstitutionalItem item = InstitutionalItem.builder()
                .title(finalTitle)
                .summary(summary)
                .authors(authors)
                .department(department)
                .year(year)
                .tags(tags)
                .type(finalType)
                .downloadUrl(publicUrl)   // frontend uses this for View/Download
                .thumbnailUrl(null)
                .addedBy(addedBy)
                .build();

        return repo.save(item);
    }

    /** Delete DB row and physical file if we can resolve it from the stored URL. */
    public void deleteItem(Long id) {
        InstitutionalItem item = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("InstitutionalItem not found: " + id));

        // Try to delete the physical file (best effort)
        String url = item.getDownloadUrl();
        String storedFilename = extractStoredFilename(url);
        if (storedFilename != null) {
            try { fileStorageService.delete(storedFilename); } catch (Exception ignored) {}
        }

        repo.delete(item);
    }

    /* ===================== Helpers & Controller-facing utilities ===================== */

    /** Convert string to enum Type (exposed for controllers). */
    public Type getType(String val) {
        return parseType(val);
    }

    /** Load an item or throw (used by controllers). */
    public InstitutionalItem getExisting(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("InstitutionalItem not found: " + id));
    }

    /** Save/update an item (used by controllers). */
    public InstitutionalItem save(InstitutionalItem item) {
        return repo.save(item);
    }

    /* ===================== Internal helpers ===================== */

    private Type parseType(String val) {
        if (!StringUtils.hasText(val)) return null;
        String v = val.trim().toUpperCase().replace(' ', '_');
        try { return Type.valueOf(v); } catch (IllegalArgumentException ex) { return null; }
    }

    /**
     * Accepts any of:
     *  - /api/repository/files/<name>
     *  - /api/repository/files/<name>/download
     *  - just <name>
     */
    private String extractStoredFilename(String url) {
        if (!StringUtils.hasText(url)) return null;
        String u = url.trim();
        if (u.endsWith("/download")) {
            u = u.substring(0, u.length() - "/download".length());
        }
        int pos = u.indexOf("/files/");
        if (pos >= 0) {
            return u.substring(pos + "/files/".length());
        }
        if (!u.contains("/")) return u;
        return null;
    }

    private String buildPublicUrl(String storedName, String folder) {
        if (StringUtils.hasText(folder)) {
            return "/api/repository/files/" + folder + "/" + storedName;
        }
        return "/api/repository/files/" + storedName;
    }

    private boolean isPdf(MultipartFile f) {
    if (f == null || f.isEmpty()) return false;

    // --- Safe content-type check ---
    String contentType = f.getContentType();
    if (contentType != null && contentType.toLowerCase().contains("pdf")) {
        return true;
    }

    // --- Safe filename check ---
    String name = f.getOriginalFilename();
    if (name != null && name.toLowerCase().endsWith(".pdf")) {
        return true;
    }

    // --- Optional quick magic number check (%PDF-) ---
    try {
        var in = f.getInputStream();
        byte[] head = new byte[5];
        int n = in.read(head);
        in.close();
        if (n == 5) {
            String sig = new String(head);
            return sig.startsWith("%PDF-");
        }
    } catch (Exception ignored) {}

    return false;
}
}
