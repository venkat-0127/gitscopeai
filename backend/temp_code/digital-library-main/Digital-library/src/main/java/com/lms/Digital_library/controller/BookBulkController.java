package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.Book;
import com.lms.Digital_library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Bulk book import + downloadable template.
 *
 * Upload is CSV-only (not real Excel).
 */
@RestController
@RequestMapping("/api/books")
@CrossOrigin
public class BookBulkController {

    @Autowired
    private BookRepository bookRepo;

    /*
     * ==========================================================
     * BULK UPLOAD (CSV only)
     * ----------------------------------------------------------
     * Expected columns (in order):
     * title,author,branch,year,available,rack,category,pdfPath,id(optional)
     *
     * If id column is present:
     * - if book with that id exists -> UPDATE that book
     * - if it does not exist -> INSERT as new book (id not forced)
     * ==========================================================
     */

    @PostMapping("/bulk")
    public ResponseEntity<?> bulk(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "program", required = false) String programOverride) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "File required", "inserted", 0, "updated", 0, "failed", 0));
        }

        int inserted = 0;
        int updated = 0;
        int failed = 0;

        List<Map<String, Object>> failedRows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean header = true;
            int rowNumber = 0;

            while ((line = br.readLine()) != null) {
                rowNumber++;

                // Skip header row
                if (header) {
                    header = false;
                    continue;
                }
                if (line.trim().isEmpty())
                    continue;

                String[] c = line.split(",", -1); // keep empty columns

                if (c.length < 7) {
                    failed++;
                    failedRows.add(Map.of(
                            "row", rowNumber,
                            "error", "Not enough columns, expected at least 7"));
                    continue;
                }

                try {
                    // ---- read values from CSV ----
                    String title = c[0].trim();
                    String author = c[1].trim();
                    String branch = c[2].trim();
                    String yearStr = c[3].trim();
                    String availStr = c[4].trim();
                    String rack = c[5].trim();
                    String category = c[6].trim();
                    String pdfPath = (c.length > 7) ? c[7].trim() : "";

                    Integer csvId = null;
                    if (c.length > 8 && !c[8].trim().isEmpty()) {
                        csvId = Integer.parseInt(c[8].trim());
                    }

                    // ---- decide INSERT vs UPDATE ----
                    Book b;
                    boolean isUpdate = false;

                    if (csvId != null && bookRepo.existsById(csvId)) {
                        // UPDATE existing book
                        b = bookRepo.findById(csvId).orElseThrow();
                        isUpdate = true;
                    } else {
                        // INSERT new book - DO NOT set id, let DB generate it
                        b = new Book();
                    }

                    // ---- apply fields ----
                    b.setTitle(title);
                    b.setAuthor(author);
                    b.setBranch(branch.isEmpty() ? null : branch);
                    b.setYear(yearStr.isEmpty() ? 0 : Integer.parseInt(yearStr));
                    b.setAvailable(availStr.isEmpty() ? 0 : Integer.parseInt(availStr));
                    b.setRack(rack);

                    if (programOverride != null
                            && !programOverride.isBlank()
                            && !"AUTO".equalsIgnoreCase(programOverride)) {
                        b.setCategory(programOverride.trim());
                    } else {
                        b.setCategory(category);
                    }

                    b.setPdfPath(pdfPath.isEmpty() ? null : pdfPath);

                    // 🔧 IMPORTANT: keep DB happy – semester column is NOT NULL
                    // We don't use semester in UI anymore, so force it to 0.
                    if (b.getSemester() == null) {
                        b.setSemester(0);
                    }

                    // ---- save ----
                    bookRepo.save(b);

                    if (isUpdate)
                        updated++;
                    else
                        inserted++;

                } catch (Exception e) {
                    failed++;
                    failedRows.add(Map.of(
                            "row", rowNumber,
                            "error", e.getMessage() != null ? e.getMessage() : "Parse/DB error"));
                }
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Parse failed: " + e.getMessage()));
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("inserted", inserted);
        resp.put("updated", updated);
        resp.put("failed", failed);
        resp.put("failedRows", failedRows);

        return ResponseEntity.ok(resp);
    }

    /*
     * ==========================================================
     * TEMPLATE DOWNLOAD
     * ==========================================================
     */

    // Main template endpoint
    @GetMapping("/bulk/template")
    public ResponseEntity<byte[]> bulkTemplate(
            @RequestParam(defaultValue = "csv") String type) {
        return buildCsvTemplate(type);
    }

    // Backwards-compatible endpoints used by your JS
    @GetMapping("/template.csv")
    public ResponseEntity<byte[]> legacyCsvTemplate() {
        return buildCsvTemplate("csv");
    }

    @GetMapping("/template.xlsx")
    public ResponseEntity<byte[]> legacyXlsxTemplate() {
        return buildCsvTemplate("xlsx");
    }

    private ResponseEntity<byte[]> buildCsvTemplate(String type) {
        String csv = "title,author,branch,year,available,rack,category,pdfPath,id(optional)\n";

        byte[] data = csv.getBytes(StandardCharsets.UTF_8);

        String fileName = "books_template.csv";
        if ("xlsx".equalsIgnoreCase(type)) {
            fileName = "books_template.xlsx"; // still CSV data, just a nicer name
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        headers.setContentType(MediaType.TEXT_PLAIN);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}
