package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs", uniqueConstraints = @UniqueConstraint(columnNames = {"source","source_id"}))
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;

    @Column(name = "source_id")
    private String sourceId;

    private String title;
    private String company;
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1024)
    private String applyUrl;

    private String jobType;   // e.g., Intern, Full-time
    private String remoteType; // e.g., Remote, Onsite, Hybrid

    private LocalDate postedDate;
    private LocalDate deadline;

    private LocalDateTime fetchedAt;

    public Job() {}

    @PrePersist
    public void onCreate() {
        if (this.fetchedAt == null) this.fetchedAt = LocalDateTime.now();
    }

    // Getters & setters (all)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getRemoteType() { return remoteType; }
    public void setRemoteType(String remoteType) { this.remoteType = remoteType; }

    public LocalDate getPostedDate() { return postedDate; }
    public void setPostedDate(LocalDate postedDate) { this.postedDate = postedDate; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }

    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", source='" + source + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", title='" + title + '\'' +
                ", company='" + company + '\'' +
                ", location='" + location + '\'' +
                ", jobType='" + jobType + '\'' +
                ", remoteType='" + remoteType + '\'' +
                ", postedDate=" + postedDate +
                ", deadline=" + deadline +
                ", fetchedAt=" + fetchedAt +
                '}';
    }
}
