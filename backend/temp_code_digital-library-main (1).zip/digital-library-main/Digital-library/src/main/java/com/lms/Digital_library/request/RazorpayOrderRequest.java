package com.lms.Digital_library.request;

import java.math.BigDecimal;

public class RazorpayOrderRequest {
    private Integer borrowId;
    private BigDecimal amount; // rupees

    public Integer getBorrowId() { return borrowId; }
    public void setBorrowId(Integer borrowId) { this.borrowId = borrowId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
