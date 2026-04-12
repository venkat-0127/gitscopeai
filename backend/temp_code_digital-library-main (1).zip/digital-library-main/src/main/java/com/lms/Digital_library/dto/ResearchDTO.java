package com.lms.Digital_library.dto;

import java.time.Instant;

/**
 * Data Transfer Object (DTO) for exposing research paper information
 * to the frontend without leaking internal entity details.
 */
public class ResearchDTO {

    private Long id;
    private String title;
    private String authors;
    private Integer year;
    private String tags;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private Instant uploadedAt;

    // Extra fields for frontend convenience
    private String downloadUrl;  // link for downloading
    private String uploadedBy;   // who uploaded the paper

    public ResearchDTO() {}

    // ===== Getters and Setters =====
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

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}
