package com.lms.Digital_library.controller;

import com.lms.Digital_library.dto.ProjectItemDTO;
import com.lms.Digital_library.entity.GithubToken;
import com.lms.Digital_library.entity.ProjectImport;
import com.lms.Digital_library.entity.ProjectItem;
import com.lms.Digital_library.repository.GithubTokenRepository;
import com.lms.Digital_library.repository.ProjectImportRepository;
import com.lms.Digital_library.repository.ProjectItemRepository;
import com.lms.Digital_library.service.GithubService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller exposing GitHub integration + Project Showcase endpoints.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GithubController {

    private static final Logger log = LoggerFactory.getLogger(GithubController.class);

    @Autowired
    private GithubTokenRepository tokenRepo;

    @Autowired
    private GithubService githubService;

    @Autowired
    private ProjectImportRepository projectRepo;

    @Autowired
    private ProjectItemRepository projectItemRepo;

    // ==================== 1) OAuth-based user repositories ====================

    @GetMapping("/github/repos")
    public ResponseEntity<List<Map<String, Object>>> listRepos(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String username = principal.getName();
        Optional<GithubToken> tokenOpt = tokenRepo.findByUsernameAndProvider(username, "github");
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String accessToken = tokenOpt.get().getAccessToken();
        try {
            List<Map<String, Object>> repos = githubService.listUserRepos(accessToken);
            return ResponseEntity.ok(repos != null ? repos : Collections.emptyList());
        } catch (Exception ex) {
            log.error("Failed to fetch GitHub repos for user {}: {}", username, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    // ==================== 2) Import GitHub repo metadata ====================

    @PostMapping("/projects/import")
    public ResponseEntity<?> importRepo(Principal principal, @RequestBody Map<String, String> body) {
        String repoFull = body != null ? body.get("repoFullName") : null;
        if (repoFull == null || repoFull.isBlank() || !repoFull.contains("/")) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Invalid 'repoFullName'"));
        }

        String[] parts = repoFull.split("/", 2);
        String owner = parts[0].trim();
        String repo = parts[1].trim();

        String username = principal != null ? principal.getName() : null;
        Optional<GithubToken> tokenOpt = (username != null)
                ? tokenRepo.findByUsernameAndProvider(username, "github")
                : Optional.empty();
        String accessToken = tokenOpt.map(GithubToken::getAccessToken).orElse(null);

        try {
            Map<String, Object> meta = githubService.getRepo(owner, repo, accessToken);
            if (meta == null || meta.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("ok", false, "message", "Repository not found"));
            }

            ProjectImport p = new ProjectImport();
            p.setTitle((String) meta.getOrDefault("name", repo));
            p.setDescription(meta.get("description") != null ? String.valueOf(meta.get("description")) : null);

            String ownerLogin = null;
            Object ownerObj = meta.get("owner");
            if (ownerObj instanceof Map<?, ?> m) {
                Object loginVal = m.get("login");
                if (loginVal != null) ownerLogin = String.valueOf(loginVal);
            }
            p.setOwnerName(ownerLogin != null ? ownerLogin : owner);
            p.setRepoFullName(repoFull);
            p.setRepoUrl(meta.get("html_url") != null ? String.valueOf(meta.get("html_url")) : null);
            p.setImportedByUserId(null);

            projectRepo.save(p);
            return ResponseEntity.ok(Map.of("ok", true, "project", p));
        } catch (Exception ex) {
            log.error("Failed to import repo {}: {}", repoFull, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "message", "Import failed"));
        }
    }

    // ==================== 3) List imported projects ====================

    @GetMapping("/projects/list")
    public ResponseEntity<Page<ProjectImport>> listProjects(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("id").descending());
        Page<ProjectImport> result;
        if (q == null || q.isBlank()) {
            result = projectRepo.findAll(pageable);
        } else {
            result = projectRepo.findByTitleContainingIgnoreCaseOrOwnerNameContainingIgnoreCase(q, q, pageable);
        }
        return ResponseEntity.ok(result);
    }

    // ==================== 4) Project Showcase CRUD ====================

    // List (paged, searchable, optional category)
    @GetMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getProjectShowcase(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "q") String query
    ) {
        ProjectItem.ProjectCategory cat = null;
        if (category != null && !category.isBlank()) {
            try {
                cat = ProjectItem.ProjectCategory.valueOf(category.toUpperCase());
            } catch (Exception ignored) {}
        }

        Page<ProjectItem> res = projectItemRepo.search(
                cat,
                (query == null || query.isBlank()) ? null : query,
                PageRequest.of(page, size)
        );

        List<ProjectItemDTO> dto = res.getContent().stream()
                .map(ProjectItemDTO::from)
                .collect(Collectors.toList());

        return Map.of(
                "content", dto,
                "totalElements", res.getTotalElements(),
                "totalPages", res.getTotalPages(),
                "page", res.getNumber(),
                "size", res.getSize()
        );
    }

    // Get by ID (useful for edit forms, optional)
    @GetMapping(value = "/projects/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectItemDTO> getOne(@PathVariable Long id) {
        return projectItemRepo.findById(id)
                .map(p -> ResponseEntity.ok(ProjectItemDTO.from(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create
    @PostMapping(value = "/projects", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectItemDTO addProject(@RequestBody ProjectItem body) {
        return ProjectItemDTO.from(projectItemRepo.save(body));
    }

    // Update (full update for fields provided)
    @PutMapping(value = "/projects/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectItemDTO> updateProject(@PathVariable Long id, @RequestBody ProjectItem body) {
        Optional<ProjectItem> opt = projectItemRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        ProjectItem existing = opt.get();
        existing.setTitle(body.getTitle());
        existing.setOwner(body.getOwner());
        existing.setTechStack(body.getTechStack());
        existing.setYear(body.getYear());
        existing.setGithubUrl(body.getGithubUrl());
        existing.setDownloadUrl(body.getDownloadUrl());
        if (body.getCategory() != null) existing.setCategory(body.getCategory());

        ProjectItem saved = projectItemRepo.save(existing);
        return ResponseEntity.ok(ProjectItemDTO.from(saved));
    }

    // Delete
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        if (!projectItemRepo.existsById(id)) return ResponseEntity.notFound().build();
        projectItemRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // ==================== 5) Public GitHub repos (no token needed) ====================

    @GetMapping("/github/public-repos")
    public Object publicRepos(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String org) {
        try {
            String url;
            if (user != null && !user.isBlank()) {
                url = "https://api.github.com/users/" + user + "/repos?per_page=100";
            } else if (org != null && !org.isBlank()) {
                url = "https://api.github.com/orgs/" + org + "/repos?per_page=100";
            } else {
                return List.of();
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(java.net.URI.create(url))
                    .header("Accept", "application/vnd.github+json")
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (Exception e) {
            log.error("GitHub public repo fetch failed: {}", e.getMessage());
            return List.of();
        }
    }
}
