package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "test_results")
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long testId;
    private String userEmail;
    private int score;
    private int total;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String detailsJson;

    private Instant createdAt = Instant.now();

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTestId() { return testId; }
    public void setTestId(Long testId) { this.testId = testId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
