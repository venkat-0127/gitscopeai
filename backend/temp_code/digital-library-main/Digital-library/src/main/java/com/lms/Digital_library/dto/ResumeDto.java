package com.lms.Digital_library.dto;

import java.time.LocalDateTime;

public class ResumeDto {
    private Long id;
    private String studentId;
    private String title;
    private String content; // raw extracted text or full html
    private String template;
    private LocalDateTime createdAt;

    // getters/setters
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
}
