package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "borrows")
public class Borrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String userEmail;
    private Integer bookId;
    private String bookTitle;
    private String status; // PENDING / APPROVED / RETURNED / PENDING_RETURN
    private Boolean returned = false;

    // fine fields
    @Column(precision = 10, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;
    private Boolean finePaid = false;

    private LocalDate borrowDate;
    private LocalDate expectedReturnDate;
    private LocalDate returnDate;
    private String bookCode;

    // getters/setters (for brevity use your IDE to generate or use Lombok)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getReturned() { return returned; }
    public void setReturned(Boolean returned) { this.returned = returned; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }

    public Boolean getFinePaid() { return finePaid; }
    public void setFinePaid(Boolean finePaid) { this.finePaid = finePaid; }

    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }

    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public String getBookCode() { return bookCode; }
    public void setBookCode(String bookCode) { this.bookCode = bookCode; }
}
