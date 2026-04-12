package com.lms.Digital_library.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * FileStorageService handles storing and retrieving uploaded files.
 * - store(MultipartFile) or store(file, subdir) stores an uploaded file.
 * - storeBytes() stores raw byte[] files.
 * - load() and loadAsResource() retrieve stored files from the root.
 * - load(dir, filename) and loadAsResource(dir, filename) retrieve from a subdirectory.
 * - loadAsResourceAnywhere()/findAnywhere() search recursively under the storage root.
 * - delete() removes a file; cleanAll() clears storage (use with caution).
 */
@Service
public class FileStorageService {

    @Value("${app.storage.location:./data/research-files}")
    private String storageLocation;

    private Path storagePath;

    @PostConstruct
    public void init() throws IOException {
        storagePath = Paths.get(storageLocation).toAbsolutePath().normalize();
        Files.createDirectories(storagePath);
    }

    /* ======================== STORE ======================== */

    /** Store a MultipartFile in the root storage and return its stored filename. */
    public String store(MultipartFile file) throws IOException {
        return store(file, null).storedFilename();
    }

    /** Store a MultipartFile (optionally under a subdirectory) and return full metadata. */
    public StoredFile store(MultipartFile file, String subdir) throws IOException {
        Objects.requireNonNull(file, "file");

        final String origRaw = file.getOriginalFilename();
        final String origName = (origRaw == null ? "" : Paths.get(origRaw).getFileName().toString());
        final String contentType = (file.getContentType() == null) ? "application/octet-stream" : file.getContentType();

        // Extract extension if present
        String ext = "";
        int dot = origName.lastIndexOf('.');
        if (dot >= 0 && dot < origName.length() - 1) {
            ext = origName.substring(dot);
        }

        // Generate a safe stored filename
        String storedName = sanitizeFilename(UUID.randomUUID().toString() + ext);

        // Create subdirectory if provided
        Path base = storagePath;
        if (subdir != null && !subdir.isBlank()) {
            String safeSubdir = sanitizeFilename(subdir);
            base = storagePath.resolve(safeSubdir).normalize();
            Files.createDirectories(base);
            if (!base.startsWith(storagePath)) {
                throw new IOException("Invalid subdirectory path");
            }
        }

        // Resolve and validate final target
        Path target = base.resolve(storedName).normalize();
        if (!target.startsWith(storagePath)) {
            throw new IOException("Invalid target path");
        }

        // Write file
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return new StoredFile(
                storedName,
                target.toAbsolutePath().toString(),
                Files.size(target),
                contentType,
                origName.isBlank() ? null : origName
        );
    }

    /** Store raw bytes with a specific filename. */
    public String storeBytes(byte[] bytes, String filename) throws IOException {
        Objects.requireNonNull(bytes, "bytes");

        if (filename == null || filename.isBlank()) {
            filename = UUID.randomUUID().toString();
        }

        if (!filename.contains(".")) {
            filename = filename + (looksLikePdf(bytes) ? ".pdf" : ".bin");
        }

        filename = sanitizeFilename(filename);
        Path target = storagePath.resolve(filename).normalize();
        if (!target.startsWith(storagePath)) {
            throw new IllegalArgumentException("Invalid filename (outside storage)");
        }

        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return filename;
    }

    /* ======================== LOAD (ROOT) ======================== */

    /** Check if a file exists (root-level only). */
    public boolean exists(String filename) {
        if (filename == null || filename.isBlank()) return false;
        Path p = load(filename);
        return p != null && Files.exists(p);
    }

    /** Resolve a stored filename to a Path (root-level). Slashes are not allowed. */
    public Path load(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }
        Path target = storagePath.resolve(filename).normalize();
        if (!target.startsWith(storagePath)) {
            throw new IllegalArgumentException("Invalid filename (outside storage)");
        }
        return target;
    }

    /** Load a file as a UrlResource for download (root-level). */
    public UrlResource loadAsResource(String filename) throws MalformedURLException {
        Path file = load(filename);
        if (file == null) {
            throw new MalformedURLException("Filename was null or blank");
        }
        UrlResource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new MalformedURLException("File not found or not readable: " + filename);
        }
        return resource;
    }

    /* ======================== LOAD (SUBDIR) ======================== */

    /** Resolve a file inside a subdirectory (dir is sanitized). */
    public Path load(String dir, String filename) {
        if (dir == null) dir = "";
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename is blank");
        }
        // Guard against traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }
        String safeDir = dir.isBlank() ? "" : sanitizeFilename(dir);
        Path base = safeDir.isBlank() ? storagePath : storagePath.resolve(safeDir).normalize();
        Path target = base.resolve(filename).normalize();
        if (!target.startsWith(storagePath)) {
            throw new IllegalArgumentException("Resolved path escapes storage root");
        }
        return target;
    }

    /** Load a file as a UrlResource from a subdirectory. */
    public UrlResource loadAsResource(String dir, String filename) throws MalformedURLException {
        Path file = load(dir, filename);
        UrlResource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new MalformedURLException("File not found or not readable: " + dir + "/" + filename);
        }
        return resource;
    }

    /* ======================== SEARCH ANYWHERE (RECURSIVE) ======================== */

    /**
     * Find a file by stored filename anywhere under storagePath (recursively).
     * Useful when you only know the generated stored filename and the file
     * might live in a type-based subfolder (e.g., "theses/").
     */
    public Path findAnywhere(String storedFilename) throws IOException {
        if (storedFilename == null || storedFilename.isBlank()) return null;
        try (Stream<Path> s = Files.walk(storagePath)) {
            return s.filter(p -> p.getFileName().toString().equals(storedFilename))
                    .findFirst()
                    .orElse(null);
        }
    }

    /** Load a UrlResource by searching recursively (works for subfolders). */
    public UrlResource loadAsResourceAnywhere(String storedFilename) throws IOException {
        Path p = findAnywhere(storedFilename);
        if (p == null) throw new MalformedURLException("File not found: " + storedFilename);
        UrlResource resource = new UrlResource(p.toUri());
        if (!resource.exists() || !resource.isReadable())
            throw new MalformedURLException("File not readable: " + storedFilename);
        return resource;
    }

    /* ======================== DELETE / CLEAN ======================== */

    /** Delete a stored file (root-level). */
    public boolean delete(String filename) throws IOException {
        if (filename == null || filename.isBlank()) return false;
        Path p = load(filename);
        if (p == null) return false;
        return Files.deleteIfExists(p);
    }

    /** Delete all stored files (use with caution). */
    public void cleanAll() throws IOException {
        FileSystemUtils.deleteRecursively(storagePath);
        Files.createDirectories(storagePath);
    }

    /* ======================== HELPERS ======================== */

    private String sanitizeFilename(String filename) {
        if (filename == null) throw new IllegalArgumentException("filename");
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }
        String safe = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isBlank()) safe = UUID.randomUUID().toString() + ".bin";
        return safe;
    }

    private boolean looksLikePdf(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        return bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
    }

    /* ======================== DTO ======================== */

    /** Rich metadata returned after storing a file. */
    public record StoredFile(
            String storedFilename,
            String absolutePath,
            long size,
            String contentType,
            String originalFilename
    ) { }
}
