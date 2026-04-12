package com.lms.Digital_library.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_items")
public class ProjectItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String owner;
    private String techStack;
    private Integer year;
    private String githubUrl;
    private String downloadUrl;
    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectCategory category = ProjectCategory.SHOWCASE;

    public enum ProjectCategory {
        SHOWCASE, SAMPLE, OPEN_SOURCE, TEMPLATE, IDEA
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ProjectCategory getCategory() { return category; }
    public void setCategory(ProjectCategory category) { this.category = category; }
}
