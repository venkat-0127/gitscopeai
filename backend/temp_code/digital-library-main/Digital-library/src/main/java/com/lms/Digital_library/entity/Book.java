// Book.java
package com.lms.Digital_library.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // --- Book Details ---
    private String title;
    private String author;

    // --- Academic Metadata ---
    private Integer semester; // kept for backward compatibility

    @Column(length = 255)
    private String branch; // new field for department (CSE, EEE, MBA...)

    private Integer year;  // publication or academic year

    // --- Inventory Details ---
    private Integer available;   // number of available copies
    private String rack;         // physical shelf location

    // --- Digital Resource ---
    @Column(name = "pdf_path")
    private String pdfPath;      // file path for uploaded PDF

    // --- Category (Program Level) ---
    // e.g., B.Tech / M.Tech / MBA
    private String category;

    // ---------- Constructors ----------
    public Book() {}

    public Book(Integer id, String title, String author, Integer semester, String branch,
                Integer year, Integer available, String rack, String pdfPath, String category) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.semester = semester;
        this.branch = branch;
        this.year = year;
        this.available = available;
        this.rack = rack;
        this.pdfPath = pdfPath;
        this.category = category;
    }

    // ---------- Getters / Setters ----------
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getAvailable() { return available; }
    public void setAvailable(Integer available) { this.available = available; }

    public String getRack() { return rack; }
    public void setRack(String rack) { this.rack = rack; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
