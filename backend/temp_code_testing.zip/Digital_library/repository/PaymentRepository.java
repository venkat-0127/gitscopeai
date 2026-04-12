package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment records.
 * Provides a few convenient query methods used by the payment flow & admin
 * pages.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Find all payments made by the given payer email (the user who paid). */
    List<Payment> findByPayerEmail(String payerEmail);

    /** Backwards-compatible alias for payer email. */
    @Query("SELECT p FROM Payment p WHERE p.payerEmail = :email")
    List<Payment> findByUserEmail(@Param("email") String email);

    /** Find all payments associated with a borrow. */
    List<Payment> findByBorrowId(Long borrowId);

    /** Find payments that were recorded/paid by admin (offline). */
    List<Payment> findByPaidByAdmin(Boolean paidByAdmin);

    /** Combined filter helper. */
    List<Payment> findByBorrowIdAndPaidByAdmin(Long borrowId, Boolean paidByAdmin);

    List<Payment> findByPayerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    /* === Added for Razorpay flow === */
    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByPaymentId(String paymentId);
}
