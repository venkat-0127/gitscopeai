package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.BorrowStatus;
import com.lms.Digital_library.repository.BorrowStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminPaymentController {

    @Autowired
    private BorrowStatusRepository borrowRepo;

    // === helper: same logic as PaymentController.computeLiveFine ===
    private int computeLiveFine(BorrowStatus b) {
        if (Boolean.TRUE.equals(b.isReturned()))
            return 0;

        LocalDate expected = b.getExpectedReturnDate();
        if (expected == null)
            return 0;

        LocalDate today = LocalDate.now();
        if (!today.isAfter(expected))
            return 0;

        long daysLate = ChronoUnit.DAYS.between(expected, today);
        return (int) Math.max(0, daysLate); // ₹1 / late day
    }

    /*
     * ===========================================================
     * GET /api/admin/payments/{id}
     * Used by Payments tab "Lookup" button
     * ===========================================================
     */
    @GetMapping("/payments/{id}")
    public ResponseEntity<?> getPayment(@PathVariable int id) {

        Optional<BorrowStatus> opt = borrowRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Borrow not found"));
        }

        BorrowStatus b = opt.get();

        int fine = (b.getFineAmount() == null ? 0 : b.getFineAmount());
        if (!Boolean.TRUE.equals(b.isReturned()) && fine == 0) {
            // for display only; don’t force-set if admin just wants to see
            fine = computeLiveFine(b);
        }

        boolean paidFlag = b.getFinePaid() != null && b.getFinePaid() != 0;
        int paid = paidFlag ? fine : 0;
        int due = Math.max(0, fine - paid);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "ok");
        resp.put("borrowId", b.getId());
        resp.put("userEmail", b.getUserEmail());
        resp.put("bookTitle", b.getBookTitle());
        resp.put("borrowDate", b.getBorrowDate());
        resp.put("expectedReturnDate", b.getExpectedReturnDate());
        resp.put("fineAmount", fine);
        resp.put("finePaid", paid);
        resp.put("due", due);
        resp.put("returned", b.isReturned());
        resp.put("statusCode", b.getStatus());
        resp.put("paymentId", b.getFinePaymentId());

        return ResponseEntity.ok(resp);
    }

    /*
     * ===========================================================
     * POST /api/admin/payments/{id}/cash
     * Body (optional): { "amount": 15 }
     * Called by "Mark Cash Paid" button
     * ===========================================================
     */
    @PostMapping("/payments/{id}/cash")
    public ResponseEntity<?> markCashPaid(
            @PathVariable int id,
            @RequestBody(required = false) Map<String, Object> body) {
        Optional<BorrowStatus> opt = borrowRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Borrow not found"));
        }

        BorrowStatus b = opt.get();

        int amount = 0;
        if (body != null && body.get("amount") != null) {
            try {
                amount = Math.max(0, Integer.parseInt(body.get("amount").toString()));
            } catch (Exception ignored) {
            }
        }

        // If DB fine was never set, sync it to either amount or live fine
        if (b.getFineAmount() == null || b.getFineAmount() == 0) {
            if (amount > 0) {
                b.setFineAmount(amount);
            } else {
                b.setFineAmount(computeLiveFine(b));
            }
        }

        b.setFinePaid(1);
        try {
            b.setFinePaymentId("CASH-" + System.currentTimeMillis());
        } catch (Exception ignored) {
        }

        borrowRepo.save(b);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Fine recorded as cash paid",
                "fineAmount", b.getFineAmount(),
                "paymentId", b.getFinePaymentId()));
    }

    /*
     * ===========================================================
     * POST /api/admin/payments/{id}/lost
     * Body: { "extra": 200, "mode": "NONE"|"CASH"|"RZP" }
     * Used by the Lost modal buttons (Add only / Add & Cash / Add & Razorpay)
     * ===========================================================
     */
    @PostMapping("/payments/{id}/lost")
    public ResponseEntity<?> markLost(
            @PathVariable int id,
            @RequestBody(required = false) Map<String, Object> body) {
        Optional<BorrowStatus> opt = borrowRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Borrow not found"));
        }

        BorrowStatus b = opt.get();

        int extra = 0;
        if (body != null && body.get("extra") != null) {
            try {
                extra = Math.max(0, Integer.parseInt(body.get("extra").toString()));
            } catch (Exception ignored) {
            }
        }

        int base = (b.getFineAmount() == null || b.getFineAmount() == 0)
                ? computeLiveFine(b)
                : b.getFineAmount();

        int total = base + extra;
        b.setFineAmount(total);

        String mode = (body != null && body.get("mode") != null)
                ? String.valueOf(body.get("mode")).toUpperCase()
                : "NONE";

        if ("CASH".equals(mode)) {
            b.setFinePaid(1);
            b.setFinePaymentId("LOST-CASH-" + System.currentTimeMillis());
            b.setStatus("LOST_SETTLED");
        } else if ("RZP".equals(mode)) {
            b.setFinePaid(1);
            b.setFinePaymentId("LOST-RZP-" + System.currentTimeMillis());
            b.setStatus("LOST_SETTLED");
        } else {
            // only record extra fine; payment will be handled later
            b.setStatus("LOST");
        }

        borrowRepo.save(b);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Lost recorded",
                "fineAmount", total,
                "paid", b.getFinePaid() != null && b.getFinePaid() != 0,
                "paymentId", b.getFinePaymentId(),
                "statusCode", b.getStatus()));
    }
}
