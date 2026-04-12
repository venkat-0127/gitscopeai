package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "news_articles")
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String title;

    @Column(length = 2000)
    private String summary;

    // Publication (The Hindu, TOI, etc.)
    @Column(name = "pub")
    private String pub;

    private Integer year;

    // epaper / editorials / clippings / govt / current
    private String type;

    // comma-separated tag list
    private String tags;

    // Where to open/read it
    private String url;

    // PDF download link (optional)
    private String downloadUrl;

    private Integer views = 0;

    private LocalDate createdAt = LocalDate.now();

    /* Getters/Setters */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getPub() { return pub; }
    public void setPub(String pub) { this.pub = pub; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}
