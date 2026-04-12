package com.lms.Digital_library.dto;

public class SearchItemDTO {
    private String id;           // external or internal id
    private String type;         // VIDEO, EBOOK, COURSE, WEBSITE, DATASET, TOOL, OTHER
    private String title;
    private String description;
    private String author;       // author / channel
    private Integer year;
    private String provider;     // YouTube, Google Books, Internal, etc.
    private String url;          // canonical page
    private String thumbnail;    // preview image
    private String extra;        // e.g., youtubeId

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
}
    