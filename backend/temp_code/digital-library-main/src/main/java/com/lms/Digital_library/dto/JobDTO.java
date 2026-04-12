package com.lms.Digital_library.dto;

import java.time.LocalDate;

public class JobDTO {
    private Long id;
    private String title;
    private String company;
    private String location;
    private String applyUrl;
    private LocalDate postedDate;
    private String jobType;

    public JobDTO() {}

    public JobDTO(Long id, String title, String company, String location, String applyUrl, LocalDate postedDate, String jobType) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.applyUrl = applyUrl;
        this.postedDate = postedDate;
        this.jobType = jobType;
    }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }
    public LocalDate getPostedDate() { return postedDate; }
    public void setPostedDate(LocalDate postedDate) { this.postedDate = postedDate; }
    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
}
