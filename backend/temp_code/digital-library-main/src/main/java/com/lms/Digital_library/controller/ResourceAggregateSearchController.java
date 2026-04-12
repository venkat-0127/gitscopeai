package com.lms.Digital_library.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.Digital_library.dto.SearchItemDTO;
import com.lms.Digital_library.entity.DigitalResource;
import com.lms.Digital_library.repository.DigitalResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified search: Internal DB + Google Books + YouTube
 */
@RestController
@RequestMapping("/api/resources")
@CrossOrigin(origins = "*")
public class ResourceAggregateSearchController {

    private static final Logger log = LoggerFactory.getLogger(ResourceAggregateSearchController.class);

    private final DigitalResourceRepository repo;           // may be null if you don't wire it
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    /** Put this in application.properties: yt.api.key=YOUR_KEY */
    @Value("${yt.api.key:}")
    private String ytApiKey;

    public ResourceAggregateSearchController(DigitalResourceRepository repo) {
        this.repo = repo;
    }

    @GetMapping(value = "/aggregate-search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> aggregate(
            @RequestParam String q,
            @RequestParam(defaultValue = "12") int limit
    ) {
        List<SearchItemDTO> results = new ArrayList<>();

        // 1) Internal DB (optional)
        if (repo != null) {
            try {
                var page = repo.search(q, null, PageRequest.of(0, Math.max(5, limit / 2)));
                results.addAll(page.getContent().stream().map(this::fromEntity).collect(Collectors.toList()));
            } catch (Exception e) {
                log.warn("Internal DB search failed: {}", e.getMessage());
            }
        }

        // 2) Google Books (free e-books prioritized)
        try {
            String url = "https://www.googleapis.com/books/v1/volumes?q=" + enc(q)
                    + "&maxResults=" + Math.min(limit, 10)
                    + "&printType=books&filter=free-ebooks";
            var body = httpGet(url);
            results.addAll(parseGoogleBooks(body));
        } catch (Exception e) {
            log.warn("Google Books failed: {}", e.getMessage());
        }

        // 3) YouTube (video lectures). Requires yt.api.key
        if (ytApiKey != null && !ytApiKey.isBlank()) {
            try {
                String url = "https://www.googleapis.com/youtube/v3/search?part=snippet"
                        + "&q=" + enc(q + " lecture")
                        + "&type=video&videoDuration=any&maxResults=" + Math.min(limit, 10)
                        + "&key=" + ytApiKey;
                var body = httpGet(url);
                results.addAll(parseYouTube(body));
            } catch (Exception e) {
                log.warn("YouTube failed: {}", e.getMessage());
            }
        } else {
            log.info("yt.api.key not configured; skipping YouTube search.");
        }

        // De-duplicate by title+provider+url (first occurrence wins)
        LinkedHashMap<String, SearchItemDTO> uniq = new LinkedHashMap<>();
        for (SearchItemDTO s : results) {
            String key = (nz(s.getTitle()) + "|" + nz(s.getProvider()) + "|" + nz(s.getUrl())).toLowerCase();
            uniq.putIfAbsent(key, s);
        }
        List<SearchItemDTO> list = new ArrayList<>(uniq.values());

        // Sort: VIDEO first, EBOOK second, then others; newer year first
        list.sort((a, b) -> {
            int wa = weight(a.getType());
            int wb = weight(b.getType());
            if (wa != wb) return Integer.compare(wa, wb);
            int ya = a.getYear() != null ? a.getYear() : Year.now().getValue() - 30;
            int yb = b.getYear() != null ? b.getYear() : Year.now().getValue() - 30;
            return Integer.compare(yb, ya);
        });

        // Cap size
        int cap = Math.max(limit, 12) * 3;
        if (list.size() > cap) list = list.subList(0, cap);

        return Map.of("content", list, "total", list.size());
    }

    /* ---------- Helpers ---------- */

    private SearchItemDTO fromEntity(DigitalResource r) {
        SearchItemDTO d = new SearchItemDTO();
        d.setId(r.getId() != null ? String.valueOf(r.getId()) : null);
        d.setType(mapCategory(r.getCategory())); // VIDEO/EBOOK/.../OTHER
        d.setTitle(nz(r.getTitle()));
        d.setDescription(nz(r.getDescription()));
        d.setAuthor(nz(r.getAuthors()));
        d.setYear(r.getYear());
        d.setProvider(nz(r.getProvider(), "Internal"));
        d.setUrl(nz(r.getSourceUrl()));
        d.setThumbnail(nz(r.getThumbnailUrl()));
        // you can populate canEdit based on your auth (omitted here)
        return d;
    }

    private String mapCategory(DigitalResource.Category cat) {
        if (cat == null) return "OTHER";
        // Map your entity enum to unified strings used by the UI
        return switch (cat) {
            case VIDEO -> "VIDEO";
            case EBOOK -> "EBOOK";
            case COURSE -> "COURSE";
            case DATASET -> "DATASET";
            case TOOL -> "TOOL";
            case WEBSITE -> "WEBSITE";
            case LECTURE_NOTES -> "LECTURE_NOTES";
            case QUESTION_PAPER -> "QUESTION_PAPER";
            default -> "OTHER";
        };
    }

    private List<SearchItemDTO> parseGoogleBooks(String json) throws Exception {
        List<SearchItemDTO> out = new ArrayList<>();
        JsonNode root = om.readTree(json);
        JsonNode items = root.path("items");
        if (items.isMissingNode() || !items.isArray()) return out;

        for (JsonNode it : items) {
            JsonNode vol = it.path("volumeInfo");
            String title = nz(vol.path("title").asText());
            String desc = nz(vol.path("description").asText());
            String thumb = nz(vol.path("imageLinks").path("thumbnail").asText());
            String link = nz(vol.path("infoLink").asText());
            String authors = "";
            if (vol.has("authors") && vol.get("authors").isArray()) {
                authors = String.join(", ",
                        toList(vol.get("authors")).stream().map(JsonNode::asText).collect(Collectors.toList()));
            }
            Integer year = null;
            String pub = vol.path("publishedDate").asText();
            if (pub != null && pub.length() >= 4) {
                try { year = Integer.parseInt(pub.substring(0, 4)); } catch (Exception ignored) {}
            }

            SearchItemDTO d = new SearchItemDTO();
            d.setType("EBOOK");
            d.setTitle(title);
            d.setDescription(desc);
            d.setAuthor(authors);
            d.setYear(year);
            d.setProvider("Google Books");
            d.setUrl(link);
            d.setThumbnail(thumb);
            out.add(d);
        }
        return out;
    }

    private List<SearchItemDTO> parseYouTube(String json) throws Exception {
        List<SearchItemDTO> out = new ArrayList<>();
        JsonNode root = om.readTree(json);
        JsonNode items = root.path("items");
        if (items.isMissingNode() || !items.isArray()) return out;

        for (JsonNode it : items) {
            String videoId = nz(it.path("id").path("videoId").asText());
            JsonNode sn = it.path("snippet");
            String title = nz(sn.path("title").asText());
            String channel = nz(sn.path("channelTitle").asText());
            String thumb = nz(sn.path("thumbnails").path("medium").path("url").asText());
            Integer year = null;
            String published = sn.path("publishedAt").asText();
            if (published != null && published.length() >= 4) {
                try { year = Integer.parseInt(published.substring(0, 4)); } catch (Exception ignored) {}
            }

            if (videoId.isBlank()) continue;
            SearchItemDTO d = new SearchItemDTO();
            d.setType("VIDEO");
            d.setTitle(title);
            d.setAuthor(channel);
            d.setYear(year);
            d.setProvider("YouTube");
            d.setUrl("https://www.youtube.com/watch?v=" + videoId);
            d.setThumbnail(thumb);
            d.setExtra(videoId); // UI uses this to embed the player
            out.add(d);
        }
        return out;
    }

    private String httpGet(String url) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + " from " + url + ": " + resp.body());
        }
        return resp.body();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static List<JsonNode> toList(JsonNode arr) {
        List<JsonNode> list = new ArrayList<>();
        arr.forEach(list::add);
        return list;
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static String nz(String s, String def) { return (s == null || s.isBlank()) ? def : s; }

    /** Lower number = higher priority for sorting */
    private static int weight(String type) {
        if (type == null) return 99;
        return switch (type.toUpperCase()) {
            case "VIDEO" -> 0;
            case "EBOOK" -> 1;
            case "COURSE" -> 2;
            case "DATASET" -> 3;
            default -> 10;
        };
    }
}
