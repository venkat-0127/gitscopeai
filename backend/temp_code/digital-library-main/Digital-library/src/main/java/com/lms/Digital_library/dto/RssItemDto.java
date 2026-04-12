package com.lms.Digital_library.dto;

public class RssItemDto {
    private String title;
    private String pub;          // publication/source name
    private Integer year;
    private String type;         // "Web"
    private String tags;         // comma-separated
    private String summary;
    private String url;          // article url
    private String downloadUrl;  // same as url for web items

    public RssItemDto() {}

    // getters & setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPub() { return pub; }
    public void setPub(String pub) { this.pub = pub; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}
