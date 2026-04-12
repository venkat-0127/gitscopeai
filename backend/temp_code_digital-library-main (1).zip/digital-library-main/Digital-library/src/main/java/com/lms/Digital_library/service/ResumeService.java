package com.lms.Digital_library.service;

import com.lms.Digital_library.entity.ResumeEntity;
import com.lms.Digital_library.repository.ResumeRepository;
import com.lms.Digital_library.dto.ParsedResumeDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resume service — handles upload, safe text extraction (Tika reflection) and parsing/ATS heuristics.
 */
@Service
public class ResumeService {

    private final ResumeRepository repo;

    public ResumeService(ResumeRepository repo) {
        this.repo = repo;
    }

    // ===== CRUD =====
    public ResumeEntity save(ResumeEntity r) {
        if (r.getCreatedAt() == null) r.setCreatedAt(LocalDateTime.now());
        return repo.save(r);
    }

    public List<ResumeEntity> findAll() { return repo.findAll(); }

    public List<ResumeEntity> findByStudentId(String studentId) { return repo.findByStudentId(studentId); }

    public Optional<ResumeEntity> findById(Long id) { return repo.findById(id); }

    public void delete(Long id) { repo.deleteById(id); }

    // ===== Upload + Text Extraction =====
    public ResumeEntity uploadAndSave(MultipartFile file, String studentId, String title, String template) throws IOException {
        String extracted = extractText(file);
        ResumeEntity r = new ResumeEntity();
        r.setStudentId(studentId);
        r.setTitle(title != null && !title.isBlank()
                ? title
                : (file != null && file.getOriginalFilename() != null ? file.getOriginalFilename() : "Resume"));
        r.setContent(extracted);
        r.setTemplate(template);
        r.setCreatedAt(LocalDateTime.now());
        return save(r);
    }

    /**
     * Extract text safely — uses Apache Tika via reflection if present,
     * else falls back to reading .txt files as UTF-8.
     *
     * If you want robust PDF/DOCX extraction in production add Tika dependency:
     * <dependency>
     *   <groupId>org.apache.tika</groupId>
     *   <artifactId>tika-core</artifactId>
     *   <version>2.x</version>
     * </dependency>
     */
    public String extractText(MultipartFile file) throws IOException {
        if (file == null) throw new IOException("No file provided");

        final String filename = Optional.ofNullable(file.getOriginalFilename())
                .map(String::toLowerCase)
                .orElse("");
        final String contentType = Optional.ofNullable(file.getContentType())
                .map(String::toLowerCase)
                .orElse("");

        // Try Apache Tika via reflection (so code compiles even if Tika not on classpath)
        try {
            Class<?> tikaClass = Class.forName("org.apache.tika.Tika");
            Object tikaInstance = tikaClass.getDeclaredConstructor().newInstance();
            Method parseToString = tikaClass.getMethod("parseToString", InputStream.class);

            try (InputStream is = file.getInputStream()) {
                Object result = parseToString.invoke(tikaInstance, is);
                if (result != null) return result.toString();
            }
        } catch (ClassNotFoundException ignored) {
            // Tika not provided — fallback below
        } catch (Exception ex) {
            // If file is plain text, return it; otherwise propagate
            if (filename.endsWith(".txt") || contentType.startsWith("text/")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            throw new IOException("Text extraction failed: " + ex.getMessage(), ex);
        }

        // Fallback for .txt types
        if (filename.endsWith(".txt") || contentType.startsWith("text/")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        throw new IOException("Unsupported file type. Add Apache Tika to enable PDF/DOCX parsing.");
    }

    // ===== Resume Parsing & ATS Analysis =====
    public ParsedResumeDto parseAndAnalyze(String text) {
        ParsedResumeDto parsed = new ParsedResumeDto();
        if (text == null) text = "";

        // 0) Normalize whitespace
        text = text.replace("\r", "\n");

        // If extractor returns one long line, insert heuristics to break paragraphs:
        // - add newline after sentence end followed by uppercase/digit (common after pdf->text).
        // - break before common ALL-CAPS headings (EDUCATION, EXPERIENCE...)
        // - replace long runs of multiple spaces with single space
        if (text.length() > 200 && text.split("\\r?\\n").length < 6) {
            text = text.replaceAll("([\\.\\?\\!])\\s+(?=[A-Z0-9\"'\\-])", "$1\n"); // split sentences
            // heuristics for headings that may be inline: put newline before known headings uppercase
            text = text.replaceAll("(?i)\\b(EDUCATION|EXPERIENCE|SKILLS|CERTIFICATIONS|PROJECTS|AWARDS|SUMMARY|OBJECTIVE|ACHIEVEMENTS)\\b", "\n$1");
            // break on long comma-separated lists that look like headers (e.g., name + contact)
            text = text.replaceAll("([A-Za-z]+),\\s+(?=[A-Z])", "$1,\n");
        }

        // collapse many spaces
        text = text.replaceAll("[ \\t]{2,}", " ").trim();

        // Split into cleaned lines
        String[] rawLines = text.split("\\r?\\n");
        List<String> lines = new ArrayList<>();
        for (String l : rawLines) {
            if (l == null) continue;
            String t = l.trim();
            if (!t.isEmpty()) {
                // remove repeated punctuation artifacts
                t = t.replaceAll("\\s{2,}", " ").trim();
                lines.add(t);
            }
        }

        // Name detection: prefer a short line near top without email/phone and not a long paragraph
        parsed.setName(null);
        for (int i = 0; i < Math.min(lines.size(), 8); i++) {
            String ln = lines.get(i);
            if (ln.length() > 2 && ln.length() < 60 && !ln.contains("@") && !ln.matches(".*\\d.*")) {
                // avoid picking long all-caps paragraphs as name
                if (!ln.equals(ln.toUpperCase()) || ln.split("\\s+").length <= 4) {
                    parsed.setName(ln);
                    break;
                }
            }
        }
        if (parsed.getName() == null && !lines.isEmpty()) parsed.setName(lines.get(0));

        // Find email and phone scanning first 12 lines
        Pattern emailRx = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
        Pattern phoneRx = Pattern.compile("(?:\\+?\\d{1,3}[\\s\\-])?(?:\\d{2,4}[\\s\\-])?\\d{6,12}");
        for (int i = 0; i < Math.min(lines.size(), 12); i++) {
            String ln = lines.get(i);
            if (parsed.getEmail() == null) {
                Matcher em = emailRx.matcher(ln);
                if (em.find()) parsed.setEmail(em.group());
            }
            if (parsed.getPhone() == null) {
                Matcher pm = phoneRx.matcher(ln);
                if (pm.find()) parsed.setPhone(pm.group().trim());
            }
            if (parsed.getEmail() != null && parsed.getPhone() != null) break;
        }

        // Build a section map scanning for headings or allcaps lines
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String current = "header";
        sections.put(current, new ArrayList<>());
        Set<String> headingKeywords = new HashSet<>(Arrays.asList(
                "experience","work experience","professional experience","employment",
                "education","academic","qualifications",
                "skills","technical skills",
                "certification","certifications","licenses",
                "projects","project",
                "summary","objective","career objective",
                "awards","achievements","additional","publications"
        ));

        for (String ln : lines) {
            String low = ln.toLowerCase(Locale.ROOT).replaceAll(":$", "");
            boolean isHeading = false;
            String matched = null;
            // exact/starts-with match
            for (String kw : headingKeywords) {
                if (low.equals(kw) || low.startsWith(kw + ":") || (low.contains(kw) && low.length() < 40)) {
                    isHeading = true;
                    matched = kw;
                    break;
                }
            }
            boolean allCapsShort = ln.matches("^[A-Z0-9\\s\\W]{3,}$") && ln.split("\\s+").length <= 6;

            if (isHeading || allCapsShort || ln.endsWith(":")) {
                String key = "other";
                if (matched != null) {
                    if (matched.contains("experience")) key = "experience";
                    else if (matched.contains("education")) key = "education";
                    else if (matched.contains("skill")) key = "skills";
                    else if (matched.contains("cert")) key = "certifications";
                    else if (matched.contains("project")) key = "projects";
                    else if (matched.contains("summary") || matched.contains("objective")) key = "summary";
                    else key = matched;
                } else if (allCapsShort) {
                    String lw = low;
                    if (lw.contains("experience")) key = "experience";
                    else if (lw.contains("education")) key = "education";
                    else if (lw.contains("skills")) key = "skills";
                    else if (lw.contains("cert")) key = "certifications";
                    else key = "other";
                } else {
                    // trailing colon fallback
                    String t = ln.replaceAll(":$", "").toLowerCase();
                    if (t.contains("experience")) key = "experience";
                    else if (t.contains("education")) key = "education";
                    else if (t.contains("skill")) key = "skills";
                    else if (t.contains("cert")) key = "certifications";
                    else if (t.contains("summary") || t.contains("objective")) key = "summary";
                    else key = "other";
                }
                current = key;
                sections.putIfAbsent(current, new ArrayList<>());
                continue; // skip heading line itself
            }

            sections.putIfAbsent(current, new ArrayList<>());
            sections.get(current).add(ln);
        }

        // Summary: prefer explicit section; fallback to next 2-4 header lines if available
        List<String> sumLines = sections.getOrDefault("summary", Collections.emptyList());
        if (!sumLines.isEmpty()) {
            parsed.setSummary(cleanShort(String.join(" ", sumLines)));
        } else {
            List<String> headerLines = sections.getOrDefault("header", Collections.emptyList());
            if (headerLines.size() > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < Math.min(headerLines.size(), 5); i++) sb.append(headerLines.get(i)).append(" ");
                parsed.setSummary(cleanShort(sb.toString()));
            } else {
                parsed.setSummary(null);
            }
        }

        // Skills: split by commas, bullets, pipes, slashes
        List<String> parsedSkills = new ArrayList<>();
        List<String> skillLines = sections.getOrDefault("skills", Collections.emptyList());
        if (!skillLines.isEmpty()) {
            String joined = String.join(" ", skillLines);
            String[] tokens = joined.split("[,;•\\|/\\n]");
            for (String t : tokens) {
                String s = t.trim();
                if (s.length() > 1 && s.length() < 60 && !parsedSkills.contains(s)) parsedSkills.add(s);
            }
        } else {
            // fallback scan for common tech tokens in whole text
            String lowAll = text.toLowerCase(Locale.ROOT);
            String[] tech = {"java","spring","react","python","sql","mysql","mongodb","aws","docker","javascript","html","css","sap","nlp","streamlit","pandas","tensorflow"};
            for (String t : tech) if (lowAll.contains(t) && !parsedSkills.contains(t)) parsedSkills.add(t);
        }
        parsed.getSkills().addAll(parsedSkills);

        // Certifications
        List<String> certs = sections.getOrDefault("certifications", Collections.emptyList());
        if (!certs.isEmpty()) {
            for (String c : String.join("\n", certs).split("[\\n;,-]")) {
                String s = c.trim();
                if (!s.isEmpty()) parsed.getCertifications().add(s);
            }
        }

        // Education
        List<String> edu = sections.getOrDefault("education", Collections.emptyList());
        for (String eLine : edu) {
            if (eLine == null || eLine.trim().isEmpty()) continue;
            ParsedResumeDto.Education ed = new ParsedResumeDto.Education();
            ed.setDegree(eLine);
            // try to capture year/grade heuristics
            Matcher gradeMatcher = Pattern.compile("(cgpa[:\\s]*\\d+(\\.\\d+)?)|(gpa[:\\s]*\\d+(\\.\\d+)?)|(\\d{4})|([0-9]{1,2}%)", Pattern.CASE_INSENSITIVE).matcher(eLine);
            if (gradeMatcher.find()) ed.setGrade(gradeMatcher.group());
            parsed.addEducation(ed);
        }

        // Experience: break into blocks by blank-line or by common separators
        List<String> experienceLines = sections.getOrDefault("experience", Collections.emptyList());
        if (!experienceLines.isEmpty()) {
            String combined = String.join("\n", experienceLines);
            String[] blocks = combined.split("\\n{1,2}");
            for (String block : blocks) {
                String b = block.trim();
                if (b.isEmpty()) continue;
                ParsedResumeDto.Experience ex = new ParsedResumeDto.Experience();
                String[] blockLines = b.split("\\r?\\n");
                // first line as title/company
                String first = blockLines[0];
                String[] parts = first.split("\\s+at\\s+|\\s+-\\s+|\\s+\\|\\s+", 2);
                ex.setTitle(parts[0].trim());
                if (parts.length > 1) ex.setCompany(parts[1].trim());
                // remaining lines are bullets if they look like bullets
                for (int i = 1; i < blockLines.length; i++) {
                    String L = blockLines[i].replaceAll("^[-•\\s]+", "").trim();
                    if (!L.isEmpty()) ex.addBullet(L);
                }
                parsed.addExperience(ex);
            }
        }

        // If still missing summary, try to build one from header (first couple of header lines)
        if ((parsed.getSummary() == null || parsed.getSummary().trim().isEmpty())) {
            List<String> header = sections.getOrDefault("header", Collections.emptyList());
            if (header.size() > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(header.size(), 4); i++) sb.append(header.get(i)).append(" ");
                parsed.setSummary(cleanShort(sb.toString()));
            }
        }

        // run ATS heuristics and suggestions
        runAtsAndSuggestions(parsed, text);

        return parsed;
    }

    private void runAtsAndSuggestions(ParsedResumeDto parsed, String fullText) {
        List<String> suggestions = new ArrayList<>();
        int score = 50;

        if (parsed.getEmail() != null && !parsed.getEmail().isBlank()) score += 10;
        else suggestions.add("Add a valid email address.");

        if (parsed.getPhone() != null && !parsed.getPhone().isBlank()) score += 10;
        else suggestions.add("Add a phone number.");

        int skillCount = parsed.getSkills().size();
        score += Math.min(20, skillCount * 2);
        if (skillCount < 5) suggestions.add("Add more relevant technical skills (aim for 6–12).");

        // Experience action verbs and bullets
        int bullets = 0, actions = 0;
        List<String> verbs = Arrays.asList("led","developed","designed","implemented","built","managed","optimized","created","improved","reduced","increased","spearheaded","automated");
        for (ParsedResumeDto.Experience e : parsed.getExperience()) {
            List<String> bList = e.getBullets();
            if (bList == null) continue;
            for (String b : bList) {
                if (b == null || b.trim().isEmpty()) continue;
                bullets++;
                String w = b.split("\\s+")[0].replaceAll("[^A-Za-z]", "").toLowerCase(Locale.ROOT);
                if (verbs.contains(w)) actions++;
            }
        }
        if (bullets > 0) {
            if ((double) actions / bullets < 0.6) suggestions.add("Use strong action verbs (e.g., Led, Built, Implemented).");
            score += Math.min(10, actions * 2);
        } else {
            suggestions.add("Add 3–5 achievement bullets per role where possible.");
        }

        // Quantifiable metrics
        Matcher m = Pattern.compile("\\d+%?|\\d+k|\\d{2,}").matcher(fullText);
        int quantified = 0;
        while (m.find()) quantified++;
        score += Math.min(10, quantified * 2);
        if (quantified == 0) suggestions.add("Add measurable results (e.g., 'Reduced errors by 30%').");

        if (parsed.getSummary() == null || parsed.getSummary().length() < 30)
            suggestions.add("Add a concise summary or career objective.");

        parsed.setAtsScore(Math.min(100, score));
        parsed.getSuggestions().addAll(suggestions);
    }

    private String cleanShort(String s) {
        if (s == null) return null;
        s = s.replaceAll("\\s{2,}", " ").trim();
        return s.length() > 800 ? s.substring(0, 800) + "..." : s;
    }
}
