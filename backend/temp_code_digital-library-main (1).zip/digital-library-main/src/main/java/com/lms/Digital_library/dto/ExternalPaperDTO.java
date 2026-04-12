package com.lms.Digital_library.dto;

public class ExternalPaperDTO {
    private String id;
    private String title;
    private String authors;
    private Integer year;
    private String source;
    private String abstractText;
    private String url;
    private String pdfUrl;

    public ExternalPaperDTO() {}

    // getters / setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
}
