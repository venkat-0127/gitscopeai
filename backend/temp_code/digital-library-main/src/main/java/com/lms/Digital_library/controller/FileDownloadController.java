package com.lms.Digital_library.controller;

import com.lms.Digital_library.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/repository/files")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FileDownloadController {

    private final FileStorageService storage;

    /* =========================
       BY DIRECTORY: /{dir}/{name} and /{dir}/{name}/download
       ========================= */

    @GetMapping("/{dir}/{name:.+}")
    public ResponseEntity<Resource> openInlineByDir(
            @PathVariable String dir,
            @PathVariable String name
    ) {
        return serveFromDir(dir, name, /*attachment=*/false);
    }

    @GetMapping("/{dir}/{name:.+}/download")
    public ResponseEntity<Resource> downloadByDir(
            @PathVariable String dir,
            @PathVariable String name
    ) {
        return serveFromDir(dir, name, /*attachment=*/true);
    }

    /* =========================
       LEGACY / ANYWHERE: /{filename} and /{filename}/download
       (search recursively under storage root)
       ========================= */

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> openInlineAnywhere(@PathVariable String filename) {
        return serveAnywhere(filename, /*attachment=*/false);
    }

    @GetMapping("/{filename:.+}/download")
    public ResponseEntity<Resource> downloadAnywhere(@PathVariable String filename) {
        return serveAnywhere(filename, /*attachment=*/true);
    }

    /* =========================
       Helpers
       ========================= */

    private ResponseEntity<Resource> serveFromDir(String dir, String name, boolean attachment) {
        try {
            // Correct overloads that respect subfolders
            UrlResource resource = storage.loadAsResource(dir, name);
            Path path = storage.load(dir, name);

            return buildResponse(resource, name, path, attachment);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private ResponseEntity<Resource> serveAnywhere(String filename, boolean attachment) {
        try {
            // Search recursively so files saved in subfolders still resolve
            UrlResource resource = storage.loadAsResourceAnywhere(filename);
            Path path = storage.findAnywhere(filename); // may be null if not found, guarded below

            return buildResponse(resource, filename, path, attachment);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private ResponseEntity<Resource> buildResponse(Resource resource,
                                                   String downloadName,
                                                   Path pathForType,
                                                   boolean attachment) throws Exception {

        String dispName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8);
        ContentDisposition cd = ContentDisposition
                .builder(attachment ? "attachment" : "inline")
                .filename(dispName, StandardCharsets.UTF_8)
                .build();

        String contentType = probeContentType(pathForType);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String probeContentType(Path path) {
        if (path != null) {
            try {
                String ct = Files.probeContentType(path);
                if (ct != null) return ct;
            } catch (Exception ignored) {}
            // Fallback by extension (helps when probeContentType returns null)
            String name = path.getFileName().toString().toLowerCase();
            for (Map.Entry<String, String> e : EXT_TO_CT.entrySet()) {
                if (name.endsWith(e.getKey())) return e.getValue();
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private static final Map<String, String> EXT_TO_CT = Map.ofEntries(
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".txt", "text/plain"),
            Map.entry(".csv", "text/csv"),
            Map.entry(".json", "application/json"),
            Map.entry(".xml", "application/xml"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry(".ppt", "application/vnd.ms-powerpoint"),
            Map.entry(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry(".xls", "application/vnd.ms-excel"),
            Map.entry(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry(".zip", "application/zip"),
            Map.entry(".rar", "application/vnd.rar")
    );
}
