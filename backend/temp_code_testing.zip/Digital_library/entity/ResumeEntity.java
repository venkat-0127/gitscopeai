package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a stored resume (uploaded or generated).
 * Holds the raw text content, metadata, and optional parsed summary info.
 */
@Entity
@Table(name = "resumes")
public class ResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // student or user identifier (could be email or LMS roll no)
    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(length = 100000)
    private String content; // full extracted text

    private String template; // name of chosen resume template

    private LocalDateTime createdAt;

    // Optional fields for quick dashboard info (not required but useful)
    private Integer atsScore; // store last analyzed score if available

    @Column(length = 500)
    private String summary; // short extracted summary for quick display

    private String parsedName;
    private String parsedEmail;
    private String parsedPhone;

    public ResumeEntity() {
        this.createdAt = LocalDateTime.now();
    }

    // ---------- Getters & Setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getAtsScore() { return atsScore; }
    public void setAtsScore(Integer atsScore) { this.atsScore = atsScore; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getParsedName() { return parsedName; }
    public void setParsedName(String parsedName) { this.parsedName = parsedName; }

    public String getParsedEmail() { return parsedEmail; }
    public void setParsedEmail(String parsedEmail) { this.parsedEmail = parsedEmail; }

    public String getParsedPhone() { return parsedPhone; }
    public void setParsedPhone(String parsedPhone) { this.parsedPhone = parsedPhone; }
}
