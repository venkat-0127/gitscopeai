package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Payment entity used by the application.
 * Added getUserEmail / setUserEmail to be compatible with repository methods
 * that expect a 'userEmail' property (e.g. findByUserEmail(...)).
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // reference to Borrow record (nullable if not associated)
    @Column(name = "borrow_id")
    private Long borrowId;

    // the user who paid - database column is payer_email
    @Column(name = "payer_email", length = 200)
    private String payerEmail;

    // amount (rupees). Use BigDecimal for money.
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    // Razorpay or provider ids
    @Column(name = "payment_id", length = 120)
    private String paymentId;     // e.g. razorpay_payment_id

    @Column(name = "order_id", length = 120)
    private String orderId;       // e.g. razorpay_order_id

    // eg "upi", "card", "netbanking"
    @Column(length = 60)
    private String method;

    // capture timestamp from provider (store as Instant)
    @Column(name = "captured_at")
    private Instant capturedAt;

    // admin paid flag (if admin recorded/offline payment)
    @Column(name = "paid_by_admin")
    private Boolean paidByAdmin = Boolean.FALSE;

    // optional reference / txn id / invoice
    @Column(length = 200)
    private String reference;

    // provider name like "RAZORPAY"
    @Column(length = 60)
    private String provider;

    // human note / invoice id
    @Column(length = 255)
    private String note;

    // when this record was created (local time)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // when payment status updated (local time)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // payment status: PENDING, SUCCESS, FAILED
    @Column(length = 40)
    private String status;

    // constructors
    public Payment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // convenience constructor
    public Payment(BigDecimal amount, String payerEmail, String status, String transactionId) {
        this();
        this.amount = amount;
        this.payerEmail = payerEmail;
        this.status = status;
        this.paymentId = transactionId;
    }

    // --- getters & setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBorrowId() { return borrowId; }

    /** Primary setter that accepts Long */
    public void setBorrowId(Long borrowId) { this.borrowId = borrowId; }

    /**
     * Convenience overload: accept Integer (service code might be passing Integer).
     * Converts safely to Long (preserves null).
     */
    public void setBorrowId(Integer borrowIdInt) {
        this.borrowId = (borrowIdInt == null ? null : borrowIdInt.longValue());
    }

    /**
     * Convenience overload: accept primitive int
     * (rarely used, but avoids autobox issues).
     */
    public void setBorrowId(int borrowIdPrimitive) {
        this.borrowId = Long.valueOf(borrowIdPrimitive);
    }

    // original payerEmail accessor (keeps DB column mapping)
    public String getPayerEmail() { return payerEmail; }
    public void setPayerEmail(String payerEmail) { this.payerEmail = payerEmail; }

    // --- NEW: property accessors for "userEmail" so Spring Data findByUserEmail works ---
    // Treat them as the same underlying value as payerEmail
    public String getUserEmail() { return this.payerEmail; }
    public void setUserEmail(String userEmail) { this.payerEmail = userEmail; }

    // money
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }

    public Boolean getPaidByAdmin() { return paidByAdmin; }
    public void setPaidByAdmin(Boolean paidByAdmin) { this.paidByAdmin = paidByAdmin; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // optional helper: mark as admin-paid and set payer email
    public void markAsAdminPaid(String adminEmail, String reference, BigDecimal amount) {
        this.paidByAdmin = Boolean.TRUE;
        this.payerEmail = adminEmail;
        this.reference = reference;
        this.amount = amount;
        this.capturedAt = Instant.now();
        this.status = "SUCCESS";
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", borrowId=" + borrowId +
                ", payerEmail='" + payerEmail + '\'' +
                ", amount=" + amount +
                ", paymentId='" + paymentId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", method='" + method + '\'' +
                ", capturedAt=" + capturedAt +
                ", paidByAdmin=" + paidByAdmin +
                ", reference='" + reference + '\'' +
                ", provider='" + provider + '\'' +
                ", note='" + note + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", status='" + status + '\'' +
                '}';
    }
}
