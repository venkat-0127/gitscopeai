package com.lms.Digital_library.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repository/files")
@CrossOrigin(origins = "*")
public class FilePreviewController {

    @Value("${app.storage.location:./data/repository}")
    private String storageRoot;

    /* ======================= INLINE VIEW (with folder) ======================= */
    @GetMapping("/{folder}/{storedName}")
    public ResponseEntity<Resource> viewWithFolder(
            @PathVariable String folder,
            @PathVariable String storedName
    ) {
        return serve(storedPath(folder, storedName), /*download=*/false);
    }

    /* ======================= DOWNLOAD (with folder) ======================= */
    @GetMapping("/{folder}/{storedName}/download")
    public ResponseEntity<Resource> downloadWithFolder(
            @PathVariable String folder,
            @PathVariable String storedName
    ) {
        return serve(storedPath(folder, storedName), /*download=*/true);
    }

    /* ======================= INLINE VIEW (no folder) ======================= */
    @GetMapping("/{storedName}")
    public ResponseEntity<Resource> viewNoFolder(@PathVariable String storedName) {
        return serve(storedPath(null, storedName), /*download=*/false);
    }

    /* ======================= DOWNLOAD (no folder) ======================= */
    @GetMapping("/{storedName}/download")
    public ResponseEntity<Resource> downloadNoFolder(@PathVariable String storedName) {
        return serve(storedPath(null, storedName), /*download=*/true);
    }

    /* ======================= Core serving logic ======================= */
    private ResponseEntity<Resource> serve(Path path, boolean download) {
        // basic existence check
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }

        // only allow PDFs
        String name = path.getFileName().toString().toLowerCase();
        if (!name.endsWith(".pdf")) {
            // Since your system is PDF-only now, reject others.
            return ResponseEntity.status(415).build(); // Unsupported Media Type
        }

        Resource res = new FileSystemResource(path);
        HttpHeaders headers = new HttpHeaders();

        if (download) {
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(path.getFileName().toString())
                    .build());
            // Let the browser decide content-type; but we can hint PDF
            headers.setContentType(MediaType.APPLICATION_PDF);
        } else {
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename(path.getFileName().toString())
                    .build());
            headers.setContentType(MediaType.APPLICATION_PDF);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(res);
    }

    /* ======================= Helpers ======================= */

    /** Build a safe path under storageRoot; blocks path traversal. */
    private Path storedPath(String folder, String storedName) {
        if (!StringUtils.hasText(storedName)) return null;

        // Normalize and prevent "../" tricks
        Path root = Paths.get(storageRoot).toAbsolutePath().normalize();
        Path target = (StringUtils.hasText(folder))
                ? root.resolve(folder).resolve(storedName)
                : root.resolve(storedName);

        target = target.normalize();
        if (!target.startsWith(root)) { // path traversal guard
            return null;
        }
        return target;
    }
}
