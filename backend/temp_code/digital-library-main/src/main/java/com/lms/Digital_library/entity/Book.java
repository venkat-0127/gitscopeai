// Book.java
package com.lms.Digital_library.entity;

import jakarta.persistence.*;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ✅ Auto-generate IDs
    private int id;

    private String title;
    private String author;
    private int semester;
    private int year;
    private int available;
    private String rack;
    private String pdfPath;

    private String category;  // ✅ Added for MBA/Other classification

    public Book() {}

    public Book(int id, String title, String author, int semester, int year, int available, String rack, String pdfPath, String category) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.semester = semester;
        this.year = year;
        this.available = available;
        this.rack = rack;
        this.pdfPath = pdfPath;
        this.category = category;
    }

    // ✅ Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getAvailable() { return available; }
    public void setAvailable(int available) { this.available = available; }

    public String getRack() { return rack; }
    public void setRack(String rack) { this.rack = rack; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
