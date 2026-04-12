package com.lms.Digital_library.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * DTO representing an article/news item for the Newspaper Hub UI.
 */
public class NewsDto {

    private Long id;
    private String title;
    private String summary;
    private String link;
    private Instant publishedAt;
    private String source;
    private String language;
    private Double score;
    private Long views;
    private String thumbnailUrl;
    private List<String> tags = new ArrayList<>();

    public NewsDto() {}

    // ---------- Getters / Setters ----------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Long getViews() { return views; }
    public void setViews(Long views) { this.views = views; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public List<String> getTags() { return tags; }

    /**
     * ✅ Smart setter for 'tags' field.
     * Accepts both a comma-separated string ("tag1,tag2")
     * and a JSON array (["tag1","tag2"]).
     */
    @JsonSetter("tags")
    public void setTags(Object value) {
        if (value == null) {
            this.tags = new ArrayList<>();
        } else if (value instanceof String s) {
            this.tags = Arrays.stream(s.split(","))
                              .map(String::trim)
                              .filter(t -> !t.isEmpty())
                              .toList();
        } else if (value instanceof Collection<?> c) {
            this.tags = c.stream()
                         .map(Object::toString)
                         .map(String::trim)
                         .filter(t -> !t.isEmpty())
                         .toList();
        } else {
            this.tags = new ArrayList<>();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NewsDto)) return false;
        NewsDto newsDto = (NewsDto) o;
        return Objects.equals(id, newsDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
