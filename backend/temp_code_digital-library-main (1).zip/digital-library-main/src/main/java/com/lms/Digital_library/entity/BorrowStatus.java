package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class BorrowStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String userEmail;
    private int bookId;
    private String bookTitle;
    private LocalDate borrowDate;
    private LocalDate returnDate;
    private boolean returned;
    private String status;
    private int fine;

    // 🔽 New Fields
    private String bookCode;
    private LocalDate expectedReturnDate;
    private LocalDate requestDate;

    // ✅ Fix for your error
    private int fineAmount;      // total fine calculated
    private boolean finePaid;    // whether fine is paid or not

    public BorrowStatus() {}

    public BorrowStatus(String userEmail, int bookId, String bookTitle) {
        this.userEmail = userEmail;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.borrowDate = LocalDate.now();
        this.returned = false;
    }

    // ✅ Getters and Setters

    public int getId() { return id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public boolean isReturned() { return returned; }
    public void setReturned(boolean returned) { this.returned = returned; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getFine() { return fine; }
    public void setFine(int fine) { this.fine = fine; }

    public String getBookCode() { return bookCode; }
    public void setBookCode(String bookCode) { this.bookCode = bookCode; }

    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }

    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

    public int getFineAmount() { return fineAmount; }
    public void setFineAmount(int fineAmount) { this.fineAmount = fineAmount; }

    public boolean isFinePaid() { return finePaid; }
    public void setFinePaid(boolean finePaid) { this.finePaid = finePaid; }
}
