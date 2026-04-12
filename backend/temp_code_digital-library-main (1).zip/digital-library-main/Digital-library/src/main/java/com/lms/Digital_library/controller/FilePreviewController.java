package com.lms.Digital_library.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller that serves files for inline preview (opens in-browser).
 * NOTE: This controller intentionally uses "/api/repository/preview" base path
 * so that download endpoints can safely live under "/api/repository/files" without ambiguity.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repository/preview")
@CrossOrigin(origins = "*")
public class FilePreviewController {

    private static final Logger log = LoggerFactory.getLogger(FilePreviewController.class);

    @Value("${app.storage.location:./data/repository}")
    private String storageRoot;

    /* ======================= INLINE VIEW (with folder) ======================= */
    // Use filename pattern ".+" so dots are preserved (e.g., file.name.pdf)
    @GetMapping("/{folder}/{storedName:.+}")
    public ResponseEntity<Resource> viewWithFolder(
            @PathVariable String folder,
            @PathVariable String storedName
    ) {
        log.debug("Preview request for folder='{}' file='{}'", folder, storedName);
        return serve(storedPath(folder, storedName), /*download=*/false);
    }

    /* ======================= INLINE VIEW (no folder) ======================= */
    @GetMapping("/{storedName:.+}")
    public ResponseEntity<Resource> viewNoFolder(@PathVariable String storedName) {
        log.debug("Preview request for file='{}' (no folder)", storedName);
        return serve(storedPath(null, storedName), /*download=*/false);
    }

    /* ======================= Core serving logic ======================= */
    private ResponseEntity<Resource> serve(Path path, boolean download) {
        // basic existence check
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            log.warn("Requested file not found or invalid path: {}", path);
            return ResponseEntity.notFound().build();
        }

        String filename = path.getFileName().toString();
        // Optionally enforce PDF-only. If you prefer strict enforcement, uncomment next lines:
        // if (!filename.toLowerCase().endsWith(".pdf")) {
        //     return ResponseEntity.status(415).build(); // Unsupported Media Type
        // }

        // Try detect content type
        String contentType = null;
        try {
            contentType = Files.probeContentType(path);
        } catch (Exception e) {
            log.debug("Could not probe content type for {}, falling back to URLConnection", filename, e);
        }
        if (contentType == null) {
            contentType = URLConnection.guessContentTypeFromName(filename);
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        Resource res = new FileSystemResource(path);
        HttpHeaders headers = new HttpHeaders();

        // Inline preview hints
        headers.setContentDisposition(ContentDisposition.inline().filename(Paths.get(filename).getFileName().toString()).build());

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(res);
    }

    /* ======================= Helpers ======================= */

    /**
     * Build a safe path under storageRoot; blocks path traversal.
     */
    private Path storedPath(String folder, String storedName) {
        if (!StringUtils.hasText(storedName)) return null;

        // Normalize and prevent "../" tricks
        Path root = Paths.get(storageRoot).toAbsolutePath().normalize();
        Path target = (StringUtils.hasText(folder))
                ? root.resolve(folder).resolve(storedName)
                : root.resolve(storedName);

        target = target.normalize();
        // path traversal guard
        if (!target.startsWith(root)) {
            log.warn("Blocked path traversal attempt: root='{}' target='{}'", root, target);
            return null;
        }
        return target;
    }
}
