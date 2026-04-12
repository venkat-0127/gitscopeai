package com.lms.Digital_library.service;

import com.lms.Digital_library.dto.RssItemDto;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLConnection;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RssNewsService {

    /** Curated free feeds — edit to your liking */
    private static final Map<String, List<Source>> SOURCES = Map.of(
        "GENERAL", List.of(
            new Source("BBC News – Top Stories", "http://feeds.bbci.co.uk/news/rss.xml"),
            new Source("Al Jazeera English", "https://www.aljazeera.com/xml/rss/all.xml")
        ),
        "EDUCATION", List.of(
            new Source("Times Higher Education", "https://www.timeshighereducation.com/rss.xml")
        ),
        "SCIENCE", List.of(
            new Source("Nature – Latest", "https://www.nature.com/nature.rss"),
            new Source("Science Magazine – News", "https://www.science.org/rss/news_current.xml"),
            new Source("arXiv – Computer Science", "https://export.arxiv.org/rss/cs")
        ),
        "TECH", List.of(
            new Source("The Verge", "https://www.theverge.com/rss/index.xml"),
            new Source("TechCrunch", "https://techcrunch.com/feed/")
        ),
        "INDIA", List.of(
            new Source("The Indian Express – India", "https://indianexpress.com/section/india/feed/")
        )
    );

    /** Simple in-memory cache to avoid hitting feeds repeatedly */
    private static final long TTL_MS = 10 * 60 * 1000; // 10 minutes
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public Map<String, List<Source>> sources() { return SOURCES; }

    /** Search across feeds (all or by category), filter by keyword, return up to `limit`. */
    public List<RssItemDto> search(String q, String cat, int limit) {
        String query = q == null ? "" : q.trim().toLowerCase();
        int max = Math.max(1, Math.min(limit, 50));

        List<Source> toFetch = (cat != null && SOURCES.containsKey(cat.toUpperCase()))
                ? SOURCES.get(cat.toUpperCase())
                : SOURCES.values().stream().flatMap(List::stream).distinct().collect(Collectors.toList());

        // fetch & flatten
        List<RssItemDto> all = new ArrayList<>();
        for (Source s : toFetch) {
            all.addAll(fetchFeedCached(s));
        }

        // filter by keyword in title/summary/tags, then sort by date desc (fallback 0)
        return all.stream()
                .filter(d -> query.isEmpty()
                        || contains(d.getTitle(), query)
                        || contains(d.getSummary(), query)
                        || contains(d.getTags(), query))
                .sorted((a,b) ->
                        Optional.ofNullable(b.getYear()).orElse(0)
                                .compareTo(Optional.ofNullable(a.getYear()).orElse(0))
                )
                .limit(max)
                .collect(Collectors.toList());
    }

    private boolean contains(String s, String q){
        return s != null && s.toLowerCase().contains(q);
    }

    private List<RssItemDto> fetchFeedCached(Source src) {
        try {
            CacheEntry ce = cache.get(src.url());
            long now = System.currentTimeMillis();
            if (ce != null && (now - ce.storedAt) < TTL_MS) return ce.items;

            List<RssItemDto> fresh = fetchFeed(src);
            cache.put(src.url(), new CacheEntry(fresh, now));
            return fresh;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<RssItemDto> fetchFeed(Source src) throws Exception {
        // Use URI to avoid deprecated URL(String); also set UA + timeouts.
        URLConnection conn = URI.create(src.url())
                .toURL()
                .openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (LMS/1.0; +https://example.edu)");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);

        SyndFeedInput input = new SyndFeedInput();
        try (XmlReader reader = new XmlReader(conn)) { // XmlReader(URLConnection)
            SyndFeed feed = input.build(reader);
            List<SyndEntry> entries = feed.getEntries();

            List<RssItemDto> list = new ArrayList<>();
            for (SyndEntry e : entries) {
                RssItemDto d = new RssItemDto();
                d.setTitle(e.getTitle());
                d.setUrl(e.getLink());
                d.setDownloadUrl(e.getLink());
                d.setPub(src.name());
                d.setType("Web");

                Date pub = e.getPublishedDate() != null ? e.getPublishedDate() : e.getUpdatedDate();
                if (pub != null) {
                    ZonedDateTime zdt = pub.toInstant().atZone(ZoneId.systemDefault());
                    d.setYear(zdt.getYear());
                }

                // summary
                String summary = "";
                if (e.getDescription() != null) summary = e.getDescription().getValue();
                if (summary == null || summary.isBlank()) {
                    List<SyndContent> contents = e.getContents();
                    if (contents != null && !contents.isEmpty()) summary = contents.get(0).getValue();
                }
                d.setSummary(trimPlain(summary));

                // tags
                if (e.getCategories() != null && !e.getCategories().isEmpty()) {
                    String tags = e.getCategories().stream()
                            .map(SyndCategory::getName)
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .collect(Collectors.joining(", "));
                    d.setTags(tags);
                }

                list.add(d);
            }
            return list;
        }
    }

    private static String trimPlain(String s) {
        if (s == null) return null;
        String plain = s.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (plain.length() > 300) plain = plain.substring(0, 297) + "...";
        return plain;
    }

    /** DTO for source */
    public record Source(String name, String url) {}

    private record CacheEntry(List<RssItemDto> items, long storedAt) {}
}
