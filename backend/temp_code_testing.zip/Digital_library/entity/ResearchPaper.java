package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "research_paper")
public class ResearchPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String authors;
    private Integer year;
    private String tags;

    /**
     * May be null for external imports where PDF is not downloaded.
     */
    @Column(nullable = true, length = 255)
    private String filename;

    @Column(length = 255)
    private String originalFilename;

    private String contentType;
    private Long sizeBytes;

    private Instant uploadedAt;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    // ===== Lifecycle Hooks =====
    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }

    // ===== Getters / Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}
