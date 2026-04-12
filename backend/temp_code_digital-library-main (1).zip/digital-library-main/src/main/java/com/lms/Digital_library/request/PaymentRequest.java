package com.lms.Digital_library.request;

public class PaymentRequest {
    private Long borrowId;
    private String method;
    private String reference;
    private String payerEmail;

    public Long getBorrowId() { return borrowId; }
    public void setBorrowId(Long borrowId) { this.borrowId = borrowId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getPayerEmail() { return payerEmail; }
    public void setPayerEmail(String payerEmail) { this.payerEmail = payerEmail; }
}
