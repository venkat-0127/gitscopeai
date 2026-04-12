package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Payment records.
 * Provides a few convenient query methods used by the payment flow & admin pages.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find all payments made by the given payer email (the user who paid).
     */
    List<Payment> findByPayerEmail(String payerEmail);

    /**
     * Backwards-compatible method name used elsewhere in your code: returns same as findByPayerEmail.
     */
    @Query("SELECT p FROM Payment p WHERE p.payerEmail = :email")
    List<Payment> findByUserEmail(@Param("email") String email);

    /**
     * Find all payments associated with a borrow (useful to show the payment history for a borrow).
     * If your Borrow id is an Integer in other parts of your project you can pass Long.valueOf(borrowId).
     */
    List<Payment> findByBorrowId(Long borrowId);

    /**
     * Find payments that were recorded/paid by admin (offline payments).
     */
    List<Payment> findByPaidByAdmin(Boolean paidByAdmin);

    /**
     * Convenience query that returns payments for a borrow and filters by paidByAdmin flag.
     * Example usage: paymentRepo.findByBorrowIdAndPaidByAdmin(borrowId, Boolean.TRUE)
     */
    List<Payment> findByBorrowIdAndPaidByAdmin(Long borrowId, Boolean paidByAdmin);
}
