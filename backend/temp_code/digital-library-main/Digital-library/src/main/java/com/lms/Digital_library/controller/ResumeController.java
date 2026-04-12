package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.ResumeEntity;
import com.lms.Digital_library.dto.ParsedResumeDto;
import com.lms.Digital_library.dto.ResumeDto;
import com.lms.Digital_library.service.ResumeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ResumeController - upload/parse/save/list/manage resumes.
 *
 * Note: CORS allows all origins for development. Replace "*" with your frontend origin in production.
 */
@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = "*")
public class ResumeController {

    private final ResumeService service;

    public ResumeController(ResumeService service) {
        this.service = service;
    }

    // ---------- CRUD (JSON) ----------
    @PostMapping
    public ResponseEntity<ResumeDto> create(@RequestBody ResumeDto dto) {
        ResumeEntity ent = new ResumeEntity();
        ent.setStudentId(dto.getStudentId());
        ent.setTitle(dto.getTitle());
        ent.setContent(dto.getContent());
        ent.setTemplate(dto.getTemplate());
        ent.setCreatedAt(LocalDateTime.now());
        ResumeEntity saved = service.save(ent);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @GetMapping
    public ResponseEntity<List<ResumeDto>> list(@RequestParam(value = "studentId", required = false) String studentId) {
        List<ResumeEntity> list = (studentId != null && !studentId.isBlank())
                ? service.findByStudentId(studentId)
                : service.findAll();
        return ResponseEntity.ok(list.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeDto> get(@PathVariable Long id) {
        Optional<ResumeEntity> o = service.findById(id);
        return o.map(e -> ResponseEntity.ok(toDto(e))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeDto> update(@PathVariable Long id, @RequestBody ResumeDto dto) {
        Optional<ResumeEntity> o = service.findById(id);
        if (o.isEmpty()) return ResponseEntity.notFound().build();
        ResumeEntity ent = o.get();
        ent.setTitle(dto.getTitle());
        ent.setContent(dto.getContent());
        ent.setTemplate(dto.getTemplate());
        ent.setStudentId(dto.getStudentId());
        ResumeEntity saved = service.save(ent);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Upload & Parse endpoints ----------

    /**
     * Upload file, extract text and save resume entity.
     * Returns saved ResumeDto. If query param parse=true, returns parsed result with saved resume as well.
     *
     * Example:
     *   POST /api/resumes/upload?parse=true
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam("studentId") String studentId,
                                    @RequestParam(value = "title", required = false) String title,
                                    @RequestParam(value = "template", required = false) String template,
                                    @RequestParam(value = "parse", required = false, defaultValue = "false") boolean parse) {
        try {
            ResumeEntity saved = service.uploadAndSave(file, studentId, title, template);
            if (parse) {
                ParsedResumeDto parsed = service.parseAndAnalyze(saved.getContent());
                // return both saved and parsed
                return ResponseEntity.ok(new UploadParseResponse(toDto(saved), parsed));
            } else {
                return ResponseEntity.ok(toDto(saved));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("File parse/save failed", e.getMessage()));
        }
    }

    /**
     * Parse an uploaded file WITHOUT saving it to DB.
     * Use this from frontend to preview / auto-fill fields before saving.
     * POST /api/resumes/parse
     */
    @PostMapping("/parse")
    public ResponseEntity<?> parseFile(@RequestParam("file") MultipartFile file) {
        try {
            // note: service.extractText will use Tika if available (or fallback for .txt)
            String text = service.extractText(file);
            ParsedResumeDto parsed = service.parseAndAnalyze(text);
            return ResponseEntity.ok(parsed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("File parse failed", e.getMessage()));
        }
    }

    /**
     * Parse raw text posted by frontend (when text is extracted client-side).
     * POST /api/resumes/parseText
     */
    @PostMapping("/parseText")
    public ResponseEntity<?> parseText(@RequestBody String rawText) {
        if (rawText == null || rawText.isBlank()) return ResponseEntity.badRequest().body(new ErrorResponse("Empty text", "Request body empty"));
        ParsedResumeDto parsed = service.parseAndAnalyze(rawText);
        return ResponseEntity.ok(parsed);
    }

    // Analyze a stored resume by id (run heuristics & ATS)
    @PostMapping("/{id}/analyze")
    public ResponseEntity<?> analyze(@PathVariable Long id) {
        Optional<ResumeEntity> o = service.findById(id);
        if (o.isEmpty()) return ResponseEntity.notFound().build();
        ParsedResumeDto parsed = service.parseAndAnalyze(o.get().getContent());
        return ResponseEntity.ok(parsed);
    }

    // ---------- helpers & small DTOs used for responses ----------

    private ResumeDto toDto(ResumeEntity e) {
        ResumeDto d = new ResumeDto();
        d.setId(e.getId());
        d.setStudentId(e.getStudentId());
        d.setTitle(e.getTitle());
        d.setContent(e.getContent());
        d.setTemplate(e.getTemplate());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }

    // small wrapper returned when upload?parse=true
    public static class UploadParseResponse {
        private ResumeDto saved;
        private ParsedResumeDto parsed;

        public UploadParseResponse() { }

        public UploadParseResponse(ResumeDto saved, ParsedResumeDto parsed) {
            this.saved = saved;
            this.parsed = parsed;
        }

        public ResumeDto getSaved() { return saved; }
        public void setSaved(ResumeDto saved) { this.saved = saved; }
        public ParsedResumeDto getParsed() { return parsed; }
        public void setParsed(ParsedResumeDto parsed) { this.parsed = parsed; }
    }

    // simple error response
    public static class ErrorResponse {
        private String error;
        private String detail;
        public ErrorResponse() {}
        public ErrorResponse(String error, String detail) { this.error = error; this.detail = detail; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
    }
}
