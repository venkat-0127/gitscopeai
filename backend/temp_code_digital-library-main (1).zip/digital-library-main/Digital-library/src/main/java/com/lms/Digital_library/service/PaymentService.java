package com.lms.Digital_library.service;

import com.lms.Digital_library.entity.BorrowStatus;
import com.lms.Digital_library.entity.Payment;
import com.lms.Digital_library.repository.BorrowStatusRepository;
import com.lms.Digital_library.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Persists payment rows and (critically) "freezes" fines on successful verification,
 * so later return approvals don't recompute more dues.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final BorrowStatusRepository borrowRepo;

    /* ================== Create / Update payment rows ================== */

    @Transactional
    public Payment createSkeleton(BigDecimal amountRupees,
                                  String payerEmail,
                                  Long borrowId,
                                  String receiptOrRef,
                                  String provider) {
        Payment p = new Payment();
        p.setAmount(amountRupees);
        p.setPayerEmail(payerEmail);
        p.setBorrowId(borrowId);
        p.setReference(receiptOrRef);
        p.setProvider(provider);
        p.setStatus("PENDING");
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return paymentRepo.save(p);
    }

    @Transactional
    public void attachOrderId(Payment p, String orderId) {
        p.setOrderId(orderId);
        p.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(p);
    }

    /**
     * Called when we received a client-side success (handler) but before signature verify.
     * We just attach payment id; SUCCESS is set by finalizeStudentVerification / webhook.
     */
    @Transactional
    public void markVerified(String orderId, String paymentId) {
        paymentRepo.findByOrderId(orderId).ifPresent(p -> {
            p.setPaymentId(paymentId);
            // keep status PENDING; finalizeStudentVerification() (or webhook) will mark SUCCESS/FAILED
            p.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(p);
        });
    }

    /** Webhook/ops path if you need it separately. */
    @Transactional
    public void markPaid(String paymentId, String method) {
        paymentRepo.findByPaymentId(paymentId).ifPresent(p -> {
            p.setStatus("SUCCESS");
            p.setMethod(method);
            p.setCapturedAt(Instant.now());
            p.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(p);
        });
    }

    @Transactional
    public void markFailed(String orderId, String paymentId, String code, String description) {
        Optional<Payment> maybe = paymentRepo.findByOrderId(orderId);
        if (maybe.isEmpty() && paymentId != null) maybe = paymentRepo.findByPaymentId(paymentId);
        maybe.ifPresent(p -> {
            p.setStatus("FAILED");
            String note = (p.getNote() == null ? "" : p.getNote() + " | ")
                    + "ERR[" + code + "]: " + description;
            p.setNote(note);
            p.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(p);
        });
    }

    /* ================== Student verification success ================== */

    /**
     * Call this AFTER Razorpay signature verification succeeds for the student flow.
     * It:
     *  - sets payments.status = SUCCESS (and method if you pass it),
     *  - "freezes" the fine into BorrowStatus.fineAmount if 0/null (using live computation),
     *  - sets finePaid=1 and stores finePaymentId.
     *
     * @param borrowId              the borrow row being settled
     * @param razorpayOrderId       the RZP order id (e.g., order_XXX)
     * @param razorpayPaymentId     the RZP payment id (e.g., pay_XXX)
     * @param paymentMethodOrNull   optional (e.g., "razorpay") — leave null if unknown
     */
    @Transactional
    public void finalizeStudentVerification(Long borrowId,
                                            String razorpayOrderId,
                                            String razorpayPaymentId,
                                            String paymentMethodOrNull) {
        // 1) Update payment row to SUCCESS
        Payment p = paymentRepo.findByOrderId(razorpayOrderId)
                .orElseGet(() -> paymentRepo.findByPaymentId(razorpayPaymentId).orElse(null));
        if (p != null) {
            p.setPaymentId(razorpayPaymentId);
            p.setStatus("SUCCESS");
            if (paymentMethodOrNull != null && !paymentMethodOrNull.isBlank()) {
                p.setMethod(paymentMethodOrNull);
            }
            p.setCapturedAt(Instant.now());
            p.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(p);
        }

        // 2) Freeze fine and set paid on the borrow row
        BorrowStatus b = borrowRepo.findById(Math.toIntExact(borrowId))
                .orElseThrow(() -> new IllegalArgumentException("Borrow not found: " + borrowId));

        // If DB fineAmount hasn't been stored yet, freeze the current live fine.
        Integer dbFine = b.getFineAmount();
        if (dbFine == null || dbFine == 0) {
            int liveFine = computeLiveFine(b); // ₹1/day rule
            b.setFineAmount(liveFine);
        }

        b.setFinePaid(1);
        try { b.setFinePaymentId(razorpayPaymentId); } catch (Exception ignored) {}

        borrowRepo.save(b);

        // (Optional) trigger invoice/email here if you have services for that.
        // invoiceService.sendFinePaidInvoice(b, p);
        // emailService.sendFinePaidEmail(...);
    }

    /* ================== Helpers ================== */

    /**
     * Computes live overdue fine (₹1/day) from expectedReturnDate until today.
     * Returns 0 if not late, already returned, or expected date missing.
     */
    private int computeLiveFine(BorrowStatus b) {
        Boolean ret = b.isReturned();
        if (ret != null && ret) return 0;

        LocalDate expected = b.getExpectedReturnDate();
        if (expected == null) return 0;

        LocalDate today = LocalDate.now();
        if (!today.isAfter(expected)) return 0;

        long daysLate = ChronoUnit.DAYS.between(expected, today);
        return (int) Math.max(0, daysLate);
    }
}
