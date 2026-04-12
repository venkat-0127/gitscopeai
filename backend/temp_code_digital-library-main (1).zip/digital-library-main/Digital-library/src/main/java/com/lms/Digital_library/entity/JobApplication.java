package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;
    private String email;
    private String position;

    @Column(length = 1024)
    private String resumeLink;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String uploadedFileName;

    @Column(columnDefinition = "LONGTEXT")
    private String uploadedFileBase64;

    private String status;

    private LocalDateTime createdAt;

    public JobApplication() {}

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "received";
    }

    // ---- Getters & Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getResumeLink() { return resumeLink; }
    public void setResumeLink(String resumeLink) { this.resumeLink = resumeLink; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getUploadedFileName() { return uploadedFileName; }
    public void setUploadedFileName(String uploadedFileName) { this.uploadedFileName = uploadedFileName; }

    public String getUploadedFileBase64() { return uploadedFileBase64; }
    public void setUploadedFileBase64(String uploadedFileBase64) { this.uploadedFileBase64 = uploadedFileBase64; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "JobApplication{" +
                "id=" + id +
                ", jobId=" + jobId +
                ", email='" + email + '\'' +
                ", position='" + position + '\'' +
                ", uploadedFileName='" + uploadedFileName + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
