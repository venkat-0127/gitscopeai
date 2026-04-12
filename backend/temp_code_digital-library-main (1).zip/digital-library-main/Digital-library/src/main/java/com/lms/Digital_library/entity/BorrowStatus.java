package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "borrow_status")
public class BorrowStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // <- Integer to match your repository

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "book_id", nullable = false)
    private Integer bookId;

    @Column(name = "book_title", nullable = false)
    private String bookTitle;

    @Column(name = "book_code")
    private String bookCode;

    @Column(name = "book_category")
    private String bookCategory;

    @Column(name = "borrow_date", nullable = false)
    private LocalDate borrowDate;

    @Column(name = "expected_return_date", nullable = false)
    private LocalDate expectedReturnDate;

    @Column(name = "request_date")
    private LocalDate requestDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "returned", nullable = false)
    private boolean returned = false;

    @Column(name = "status", nullable = false)
    private String status = "REQUESTED";

    @Column(name = "fine", nullable = false)
    private Integer fine = 0;

    @Column(name = "fine_amount", nullable = false)
    private Integer fineAmount = 0;

    @Column(name = "fine_paid", nullable = false)
    private Integer finePaid = 0; // INT in DB

    @Column(name = "fine_legacy", nullable = false)
    private Integer fineLegacy = 0;

    @Column(name = "fine_payment_id")
    private String finePaymentId;

    public BorrowStatus() {}

    public BorrowStatus(String userEmail, Integer bookId, String bookTitle) {
        this.userEmail = userEmail;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.borrowDate = LocalDate.now();
        this.expectedReturnDate = LocalDate.now().plusDays(15);
        this.requestDate = LocalDate.now();
        this.returned = false;
        this.status = "REQUESTED";
        this.fine = 0;
        this.fineAmount = 0;
        this.finePaid = 0;
        this.fineLegacy = 0;
    }

    @PrePersist
    public void prePersist() {
        if (borrowDate == null) borrowDate = LocalDate.now();
        if (expectedReturnDate == null) expectedReturnDate = LocalDate.now().plusDays(15);
        if (requestDate == null) requestDate = LocalDate.now();
        if (status == null) status = "REQUESTED";
        if (bookCategory == null) bookCategory = "OTHER";
        if (fine == null) fine = 0;
        if (fineAmount == null) fineAmount = 0;
        if (finePaid == null) finePaid = 0;
        if (fineLegacy == null) fineLegacy = 0;
    }

    // ---------- Getters / Setters ----------
    public Integer getId() { return id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBookCode() { return bookCode; }
    public void setBookCode(String bookCode) { this.bookCode = bookCode; }

    public String getBookCategory() { return bookCategory; }
    public void setBookCategory(String bookCategory) { this.bookCategory = bookCategory; }

    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }

    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }

    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public boolean isReturned() { return returned; }
    public void setReturned(boolean returned) { this.returned = returned; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getFine() { return fine; }
    public void setFine(Integer fine) { this.fine = fine; }

    public Integer getFineAmount() { return fineAmount; }
    public void setFineAmount(Integer fineAmount) { this.fineAmount = fineAmount; }

    public Integer getFinePaid() { return finePaid; }
    public void setFinePaid(Integer finePaid) { this.finePaid = finePaid; }

    // Backward-compat: allow older code to call setFinePaid(boolean)
    public void setFinePaid(boolean paid) { this.finePaid = paid ? (this.fineAmount == null ? 0 : this.fineAmount) : 0; }

    public Integer getFineLegacy() { return fineLegacy; }
    public void setFineLegacy(Integer fineLegacy) { this.fineLegacy = fineLegacy; }

    public String getFinePaymentId() { return finePaymentId; }
    public void setFinePaymentId(String finePaymentId) { this.finePaymentId = finePaymentId; }
}
