package com.lms.Digital_library.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.Digital_library.dto.ExternalPaperDTO;
import com.lms.Digital_library.dto.ResearchDTO;
import com.lms.Digital_library.entity.ResearchPaper;
import com.lms.Digital_library.repository.ResearchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * External paper search + import service.
 * - fixes: imports URLEncoder; uses getStatusCode() instead of deprecated getRawStatusCode()
 */
@Service
public class ExternalPaperService {

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final FileStorageService storage;
    private final ResearchRepository repo;

    @Value("${external.ieee.apiKey:}")
    private String ieeeApiKey;

    @Value("${external.semantic.apiKey:}")
    private String semanticApiKey;

    public ExternalPaperService(WebClient.Builder webClientBuilder,
                                ObjectMapper objectMapper,
                                FileStorageService storage,
                                ResearchRepository repo) {
        this.webClient = webClientBuilder.build();
        this.mapper = objectMapper;
        this.storage = storage;
        this.repo = repo;
    }

    // =================== IMPORT ===================
    public ResearchDTO importExternal(ExternalPaperDTO dto, boolean downloadPdf) {
        if (dto == null) throw new IllegalArgumentException("Missing external paper data");

        ResearchPaper p = new ResearchPaper();
        p.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : "Untitled");
        p.setAuthors(dto.getAuthors());
        p.setYear(dto.getYear());
        String tags = "external";
        if (StringUtils.hasText(dto.getSource())) tags += "," + dto.getSource();
        p.setTags(tags);

        String orig = StringUtils.hasText(dto.getPdfUrl()) ? dto.getPdfUrl() : dto.getUrl();
        p.setOriginalFilename(orig);
        p.setUploadedAt(Instant.now());
        p.setUploadedBy("external");
        p.setContentType(null);
        p.setSizeBytes(null);

        // optionally download PDF and store
        if (downloadPdf && StringUtils.hasText(dto.getPdfUrl())) {
            try {
                byte[] bytes = webClient.get()
                        .uri(dto.getPdfUrl())
                        .accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block();

                if (bytes != null && bytes.length > 64) {
                    String guessed = guessFilenameFromUrl(dto.getPdfUrl());
                    // expects FileStorageService.storeBytes(byte[], originalFilename) to return stored filename
                    String stored = storage.storeBytes(bytes, guessed);
                    p.setFilename(stored);
                    p.setOriginalFilename(dto.getPdfUrl());
                    p.setContentType("application/pdf");
                    p.setSizeBytes((long) bytes.length);
                }
            } catch (WebClientResponseException wex) {
                // prefer getStatusCode() / getStatusCode().value() (avoid deprecated getRawStatusCode())
                System.err.println("PDF download HTTP error: " + 
                    (wex.getStatusCode() != null ? wex.getStatusCode().value() : "unknown") + " - " + wex.getMessage());
            } catch (Exception ex) {
                System.err.println("Failed to download external PDF: " + ex.getMessage());
            }
        }

        ResearchPaper saved = repo.save(p);

        ResearchDTO out = new ResearchDTO();
        out.setId(saved.getId());
        out.setTitle(saved.getTitle());
        out.setAuthors(saved.getAuthors());
        out.setYear(saved.getYear());
        out.setTags(saved.getTags());
        out.setOriginalFilename(saved.getOriginalFilename());
        out.setContentType(saved.getContentType());
        out.setSizeBytes(saved.getSizeBytes());
        out.setUploadedAt(saved.getUploadedAt());
        out.setUploadedBy(saved.getUploadedBy());

        if (StringUtils.hasText(saved.getFilename())) {
            out.setDownloadUrl("/api/research/download/" + saved.getId());
        } else if (StringUtils.hasText(dto.getPdfUrl())) {
            out.setDownloadUrl(dto.getPdfUrl());
        } else if (StringUtils.hasText(dto.getUrl())) {
            out.setDownloadUrl(dto.getUrl());
        } else {
            out.setDownloadUrl(null);
        }

        return out;
    }

    // =================== SEARCH METHODS ===================
    public List<ExternalPaperDTO> searchCrossref(String q, int rows) {
        try {
            String uri = "https://api.crossref.org/works?query=" + encode(q) + "&rows=" + rows;
            String json = webClient.get().uri(uri).retrieve().bodyToMono(String.class).block();
            if (json == null || json.isBlank()) return List.of();

            Map<String, Object> root = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Object messageObj = root.get("message");
            if (!(messageObj instanceof Map)) return List.of();
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) messageObj;

            Object itemsObj = message.get("items");
            if (!(itemsObj instanceof List)) return List.of();
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) itemsObj;

            List<ExternalPaperDTO> out = new ArrayList<>(items.size());
            for (Object o : items) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) o;
                ExternalPaperDTO d = new ExternalPaperDTO();
                d.setSource("crossref");
                d.setId(asString(m.get("DOI"), UUID.randomUUID().toString()));

                Object titles = m.get("title");
                if (titles instanceof List && !((List<?>) titles).isEmpty()) {
                    d.setTitle(String.valueOf(((List<?>) titles).get(0)));
                } else {
                    d.setTitle(asString(titles, null));
                }

                Object authorObj = m.get("author");
                if (authorObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> authorsList = (List<Object>) authorObj;
                    String auth = authorsList.stream()
                            .filter(x -> x instanceof Map)
                            .map(x -> {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> am = (Map<String, Object>) x;
                                Object given = am.getOrDefault("given", "");
                                Object family = am.getOrDefault("family", "");
                                String s = (given + " " + family).toString().trim();
                                return s.isBlank() ? null : s;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", "));
                    d.setAuthors(auth);
                }

                try {
                    Object issued = m.get("issued");
                    if (issued instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> issuedMap = (Map<String, Object>) issued;
                        Object dateParts = issuedMap.get("date-parts");
                        if (dateParts instanceof List && !((List<?>) dateParts).isEmpty()) {
                            Object first = ((List<?>) dateParts).get(0);
                            if (first instanceof List && !((List<?>) first).isEmpty()) {
                                Object y = ((List<?>) first).get(0);
                                if (y instanceof Number) d.setYear(((Number) y).intValue());
                                else d.setYear(Integer.parseInt(y.toString()));
                            }
                        }
                    }
                } catch (Exception ignore) { }

                d.setUrl(asString(m.get("URL"), null));

                try {
                    Object linkObj = m.get("link");
                    if (linkObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> linkList = (List<Object>) linkObj;
                        for (Object l : linkList) {
                            if (!(l instanceof Map)) continue;
                            @SuppressWarnings("unchecked")
                            Map<String, Object> lm = (Map<String, Object>) l;
                            Object ct = lm.get("content-type");
                            if ("application/pdf".equals(ct) || "application/pdf; charset=binary".equals(ct)) {
                                d.setPdfUrl(asString(lm.get("URL"), null));
                                break;
                            }
                        }
                    }
                } catch (Exception ignore) {}

                out.add(d);
            }
            return out;
        } catch (WebClientResponseException wex) {
            throw new IllegalStateException("CrossRef HTTP error: " + (wex.getStatusCode() != null ? wex.getStatusCode().value() : "unknown"), wex);
        } catch (Exception e) {
            throw new IllegalStateException("CrossRef search failed: " + e.getMessage(), e);
        }
    }

    public List<ExternalPaperDTO> searchArxiv(String q, int maxResults) {
        try {
            String uri = "http://export.arxiv.org/api/query?search_query=all:" + encode(q) + "&start=0&max_results=" + maxResults;
            String atom = webClient.get().uri(uri).retrieve().bodyToMono(String.class).block();
            if (atom == null || atom.isBlank()) return List.of();
            List<ExternalPaperDTO> out = new ArrayList<>();
            String[] entries = atom.split("<entry>");
            for (int i = 1; i < entries.length; i++) {
                String e = entries[i];
                ExternalPaperDTO d = new ExternalPaperDTO();
                d.setSource("arxiv");
                String idFull = textBetween(e, "<id>", "</id>");
                if (idFull != null) d.setId(idFull.replace("http://arxiv.org/abs/", "").trim());
                d.setTitle(stripTags(textBetween(e, "<title>", "</title>")));
                String[] authorParts = e.split("<author>");
                String authors = Arrays.stream(authorParts).skip(1)
                        .map(a -> stripTags(textBetween(a, "<name>", "</name>")))
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", "));
                d.setAuthors(authors);
                String published = textBetween(e, "<published>", "</published>");
                if (published != null && published.length() >= 4) {
                    try { d.setYear(Integer.parseInt(published.substring(0,4))); } catch(Exception ignored) {}
                }
                d.setUrl(textBetween(e, "<id>", "</id>"));
                String pdf = null;
                int idx = e.indexOf("title=\"pdf\"");
                if (idx > 0) {
                    int href = e.lastIndexOf("href=\"", idx);
                    if (href > 0) {
                        int start = href + 6;
                        int end = e.indexOf("\"", start);
                        if (end > start) pdf = e.substring(start, end);
                    }
                }
                d.setPdfUrl(pdf);
                out.add(d);
            }
            return out;
        } catch (WebClientResponseException wex) {
            throw new IllegalStateException("arXiv HTTP error: " + (wex.getStatusCode() != null ? wex.getStatusCode().value() : "unknown"), wex);
        } catch (Exception e) {
            throw new IllegalStateException("arXiv search failed: " + e.getMessage(), e);
        }
    }

    public List<ExternalPaperDTO> searchSemantic(String q, int limit) {
        try {
            String uri = "https://api.semanticscholar.org/graph/v1/paper/search?query=" + encode(q)
                    + "&limit=" + limit + "&fields=title,authors,year,externalIds,url,openAccessPdf";
            WebClient.RequestHeadersSpec<?> req = webClient.get().uri(uri);
            if (semanticApiKey != null && !semanticApiKey.isBlank()) {
                req = req.header("x-api-key", semanticApiKey);
            }
            String json = req.retrieve().bodyToMono(String.class).block();
            if (json == null || json.isBlank()) return List.of();

            Map<String, Object> root = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Object dataObj = root.get("data");
            if (!(dataObj instanceof List)) return List.of();
            @SuppressWarnings("unchecked")
            List<Object> data = (List<Object>) dataObj;

            List<ExternalPaperDTO> out = new ArrayList<>(data.size());
            for (Object o : data) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) o;
                ExternalPaperDTO d = new ExternalPaperDTO();
                d.setSource("semantic");
                d.setId(asString(m.getOrDefault("paperId", UUID.randomUUID().toString()), null));
                d.setTitle(asString(m.get("title"), null));
                Object authorsObj = m.get("authors");
                if (authorsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> authList = (List<Object>) authorsObj;
                    String authors = authList.stream()
                            .filter(x -> x instanceof Map)
                            .map(x -> (Map<?,?>) x)
                            .map(am -> asString(((Map<?,?>) am).get("name"), null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", "));
                    d.setAuthors(authors);
                }
                Object yr = m.get("year");
                if (yr instanceof Number) d.setYear(((Number) yr).intValue());
                Object oap = m.get("openAccessPdf");
                if (oap instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> oapm = (Map<String, Object>) oap;
                    d.setPdfUrl(asString(oapm.get("url"), null));
                }
                d.setUrl(asString(m.get("url"), null));
                out.add(d);
            }
            return out;
        } catch (WebClientResponseException wex) {
            throw new IllegalStateException("Semantic Scholar HTTP error: " + (wex.getStatusCode() != null ? wex.getStatusCode().value() : "unknown"), wex);
        } catch (Exception e) {
            throw new IllegalStateException("Semantic search failed: " + e.getMessage(), e);
        }
    }

    public List<ExternalPaperDTO> searchIeee(String q, int rows) {
        if (ieeeApiKey == null || ieeeApiKey.isBlank()) return List.of();
        try {
            // placeholder — requires IEEE API integration
            return List.of();
        } catch (Exception e) {
            throw new IllegalStateException("IEEE search failed: " + e.getMessage(), e);
        }
    }

    // ---------- Helpers ----------
    private static String textBetween(String s, String a, String b) {
        if (s == null) return null;
        int i = s.indexOf(a); if (i < 0) return null; i += a.length();
        int j = s.indexOf(b, i); if (j < 0) return s.substring(i);
        return s.substring(i, j);
    }
    private static String stripTags(String s) {
        if (s == null) return null;
        return s.replaceAll("<[^>]+>", "").trim();
    }
    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
    private static String asString(Object o, String fallback) {
        return o == null ? fallback : o.toString();
    }
    private static String guessFilenameFromUrl(String url) {
        if (url == null) return "file.pdf";
        int i = url.lastIndexOf('/');
        if (i < 0) return url;
        String s = url.substring(i + 1);
        if (s.isBlank()) return "file.pdf";
        return s.split("\\?")[0];
    }
}
