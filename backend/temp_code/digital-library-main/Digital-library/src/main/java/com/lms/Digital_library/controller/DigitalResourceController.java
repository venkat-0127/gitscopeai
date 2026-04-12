package com.lms.Digital_library.controller;

import com.lms.Digital_library.dto.DigitalResourceDTO;
import com.lms.Digital_library.entity.DigitalResource;
import com.lms.Digital_library.repository.DigitalResourceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin(origins = "*")
public class DigitalResourceController {

    private static final Logger log = LoggerFactory.getLogger(DigitalResourceController.class);

    private final DigitalResourceRepository repo;

    public DigitalResourceController(DigitalResourceRepository repo) {
        this.repo = repo;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> list(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size,
            @RequestParam(required=false) String q,
            @RequestParam(required=false) String category
    ){
        DigitalResource.Category cat = null;
        if (category != null && !category.isBlank()) {
            try { cat = DigitalResource.Category.valueOf(category.toUpperCase()); } catch (Exception ignored) {}
        }
        Page<DigitalResource> res = repo.search(q, cat, PageRequest.of(page, size, Sort.by("id").descending()));
        return Map.of(
                "content", res.getContent().stream().map(DigitalResourceDTO::from).collect(Collectors.toList()),
                "totalElements", res.getTotalElements(),
                "totalPages", res.getTotalPages(),
                "page", res.getNumber(),
                "size", res.getSize()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<DigitalResourceDTO> get(@PathVariable Long id){
        return repo.findById(id)
                .map(r -> ResponseEntity.ok(DigitalResourceDTO.from(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DigitalResourceDTO create(@RequestBody DigitalResource body){
        return DigitalResourceDTO.from(repo.save(body));
    }

    @PutMapping(value="/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DigitalResourceDTO> update(@PathVariable Long id, @RequestBody DigitalResource body){
        return repo.findById(id).map(existing -> {
            existing.setTitle(body.getTitle());
            existing.setDescription(body.getDescription());
            existing.setCategory(body.getCategory());
            existing.setTags(body.getTags());
            existing.setAuthors(body.getAuthors());
            existing.setYear(body.getYear());
            existing.setProvider(body.getProvider());
            existing.setSourceUrl(body.getSourceUrl());
            existing.setDownloadUrl(body.getDownloadUrl());
            existing.setThumbnailUrl(body.getThumbnailUrl());
            return ResponseEntity.ok(DigitalResourceDTO.from(repo.save(existing)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    /** Fetch OpenGraph-ish metadata for a URL to prefill form fields */
    @GetMapping("/import-metadata")
    public Map<String, Object> importMetadata(@RequestParam String url){
        try{
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent","Mozilla/5.0")
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String html = resp.body();

            Map<String,Object> meta = new HashMap<>();
            meta.put("title", extractMeta(html, "property=\"og:title\"", "content"));
            if (meta.get("title")==null) meta.put("title", extractMeta(html, "name=\"title\"", "content"));
            meta.put("description", extractMeta(html, "property=\"og:description\"", "content"));
            meta.put("thumbnailUrl", extractMeta(html, "property=\"og:image\"", "content"));
            meta.put("provider", hostname(url));
            meta.put("sourceUrl", url);
            return meta;
        }catch(Exception e){
            log.warn("Metadata import failed: {}", e.getMessage());
            return Map.of("sourceUrl", url);
        }
    }

    // --- tiny helpers (simple regex-free parsing for meta tags) ---
    private String extractMeta(String html, String keyFragment, String attr){
        int k = html.toLowerCase().indexOf(keyFragment.split("=")[0].toLowerCase());
        if (k<0) return null;
        int tagStart = html.lastIndexOf("<meta", k);
        int tagEnd = html.indexOf(">", k);
        if (tagStart<0 || tagEnd<0) return null;
        String tag = html.substring(tagStart, tagEnd+1);
        String attrKey = attr + "=\"";
        int v1 = tag.toLowerCase().indexOf(attrKey);
        if (v1<0) return null;
        int v2 = tag.indexOf("\"", v1 + attrKey.length());
        if (v2<0) return null;
        return tag.substring(v1 + attrKey.length(), v2).trim();
    }
    private String hostname(String url){
        try{
            String h = new java.net.URI(url).getHost();
            return (h!=null && h.startsWith("www.")) ? h.substring(4) : h;
        }catch(Exception ignored){ return null; }
    }
}
