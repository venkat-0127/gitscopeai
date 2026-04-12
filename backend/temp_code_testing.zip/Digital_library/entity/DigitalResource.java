package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "digital_resources")
public class DigitalResource {

    public enum Category {
        EBOOK, LECTURE_NOTES, VIDEO, COURSE, DATASET, TOOL, WEBSITE, PAPER, QUESTION_PAPER, OTHER
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String title;

    @Column(length=2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Category category = Category.OTHER;

    /** comma-separated tags */
    private String tags;

    private String authors;       // or instructors/creators
    private Integer year;
    private String provider;      // e.g., Coursera, Kaggle, NPTEL, Internal

    private String sourceUrl;     // where it lives
    private String downloadUrl;   // if you host / file link
    private String thumbnailUrl;  // preview image

    private String addedBy;       // username/email (optional)

    @Column(nullable=false, updatable=false)
    private Instant createdAt = Instant.now();

    // getters/setters…
    // (generate with Lombok if you use it)
    // --- omitted for brevity ---
    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public String getTitle(){ return title; }
    public void setTitle(String title){ this.title = title; }
    public String getDescription(){ return description; }
    public void setDescription(String description){ this.description = description; }
    public Category getCategory(){ return category; }
    public void setCategory(Category category){ this.category = category; }
    public String getTags(){ return tags; }
    public void setTags(String tags){ this.tags = tags; }
    public String getAuthors(){ return authors; }
    public void setAuthors(String authors){ this.authors = authors; }
    public Integer getYear(){ return year; }
    public void setYear(Integer year){ this.year = year; }
    public String getProvider(){ return provider; }
    public void setProvider(String provider){ this.provider = provider; }
    public String getSourceUrl(){ return sourceUrl; }
    public void setSourceUrl(String sourceUrl){ this.sourceUrl = sourceUrl; }
    public String getDownloadUrl(){ return downloadUrl; }
    public void setDownloadUrl(String downloadUrl){ this.downloadUrl = downloadUrl; }
    public String getThumbnailUrl(){ return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl){ this.thumbnailUrl = thumbnailUrl; }
    public String getAddedBy(){ return addedBy; }
    public void setAddedBy(String addedBy){ this.addedBy = addedBy; }
    public Instant getCreatedAt(){ return createdAt; }
    public void setCreatedAt(Instant createdAt){ this.createdAt = createdAt; }
}
