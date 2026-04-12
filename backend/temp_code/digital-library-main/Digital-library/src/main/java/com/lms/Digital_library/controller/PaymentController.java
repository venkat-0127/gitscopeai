package com.lms.Digital_library.controller;

import com.lms.Digital_library.dto.CreateOrderRequest;
import com.lms.Digital_library.dto.CreateOrderResponse;
import com.lms.Digital_library.dto.GenericResponse;
import com.lms.Digital_library.dto.RefundRequest;
import com.lms.Digital_library.dto.VerifyPaymentRequest;
import com.lms.Digital_library.entity.BorrowStatus;
import com.lms.Digital_library.repository.BorrowStatusRepository;
import com.lms.Digital_library.repository.PaymentRepository;
import com.lms.Digital_library.service.RazorpayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin
public class PaymentController {

    private final RazorpayService razorpayService;
    private final BorrowStatusRepository borrowRepo;
    private final PaymentRepository paymentRepository;

    /* ================= Razorpay core (student + admin) ================= */

    /**
     * Main create-order endpoint (student + admin).
     * Expects JSON matching your CreateOrderRequest:
     * {
     *   "amountRupees": 30,
     *   "borrowId": 19,
     *   "notes": { ... },           // optional
     *   "payerEmail": "...",
     *   "payerName": "...",
     *   "receipt": "..."
     * }
     */
    @PostMapping("/orders")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @RequestBody CreateOrderRequest req) throws Exception {

        // Enforce minimum ₹1
        BigDecimal amt = req.getAmountRupees();
        if (amt == null || amt.compareTo(BigDecimal.ONE) < 0) {
            req.setAmountRupees(BigDecimal.ONE);
        }

        // Fill default notes if missing
        ensureDefaultNotes(req, "Payment for borrow");

        return ResponseEntity.ok(razorpayService.createOrder(req));
    }

    /**
     * Backward-compatible alias if any old JS still calls /create-order.
     */
    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrderAlias(
            @RequestBody CreateOrderRequest req) throws Exception {
        return createOrder(req);
    }

    /**
     * Admin entry-point. Uses same DTO as students; just customises notes text.
     */
    @PostMapping("/admin/orders")
    public ResponseEntity<CreateOrderResponse> createAdminOrder(
            @RequestBody CreateOrderRequest req) throws Exception {

        ensureDefaultNotes(req, "Admin payment for borrow");
        return createOrder(req); // reuse main logic
    }

    /* ================= Verify / Refund ================= */

    /** Student-side (and admin-side) verification */
    @PostMapping("/verify")
    public ResponseEntity<GenericResponse> verify(
            @Valid @RequestBody VerifyPaymentRequest req) {

        boolean ok = razorpayService.verifySignature(req);
        if (!ok) {
            return ResponseEntity.badRequest().body(new GenericResponse("Signature mismatch"));
        }

        if (req.getBorrowId() == null) {
            return ResponseEntity.badRequest().body(new GenericResponse("borrowId missing"));
        }

        Optional<BorrowStatus> opt = borrowRepo.findById(req.getBorrowId());
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(new GenericResponse("Borrow not found"));
        }

        BorrowStatus b = opt.get();

        // Freeze fine in DB if still 0
        Integer dbFine = b.getFineAmount();
        if (dbFine == null || dbFine == 0) {
            int liveFine = computeLiveFine(b);
            b.setFineAmount(liveFine);
        }

        b.setFinePaid(1);
        try {
            if (req.getRazorpayPaymentId() != null) {
                b.setFinePaymentId(req.getRazorpayPaymentId());
            }
        } catch (Exception ignored) {}

        borrowRepo.save(b);

        return ResponseEntity.ok(new GenericResponse("SUCCESS"));
    }

    @PostMapping("/refunds")
    public ResponseEntity<GenericResponse> refund(
            @Valid @RequestBody RefundRequest req) throws Exception {
        String refundId = razorpayService.refund(req);
        return ResponseEntity.ok(new GenericResponse("Refund initiated: " + refundId));
    }

    /* ================= Admin-facing helpers ================= */

    /** LIVE lookup for a borrow's fine. */
    @GetMapping("/lookup")
    public ResponseEntity<?> lookup(@RequestParam int borrowId) {
        Optional<BorrowStatus> opt = borrowRepo.findById(borrowId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "error",
                    "message", "Borrow not found"
            ));
        }
        BorrowStatus b = opt.get();

        int fine = (b.getFineAmount() == null ? 0 : b.getFineAmount());
        if (!Boolean.TRUE.equals(b.isReturned()) && fine == 0) {
            fine = computeLiveFine(b); // only for display
        }

        boolean paidFlag = b.getFinePaid() != null && b.getFinePaid() != 0;
        int paid = paidFlag ? fine : 0;
        int due = Math.max(0, fine - paid);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "borrowId", b.getId(),
                "userEmail", b.getUserEmail(),
                "bookTitle", b.getBookTitle(),
                "fineAmount", fine,
                "finePaid", paid,
                "due", due
        ));
    }

    /**
     * Admin confirms online payment after Razorpay success.
     * Body: { borrowId, paymentId, amountPaise }
     */
    @PostMapping("/admin/confirm")
    public ResponseEntity<?> adminConfirm(@RequestBody Map<String, Object> body) {
        if (body == null || !body.containsKey("borrowId")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "borrowId is required"
            ));
        }
        int borrowId = Integer.parseInt(body.get("borrowId").toString());
        Optional<BorrowStatus> opt = borrowRepo.findById(borrowId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "error",
                    "message", "Borrow not found"
            ));
        }
        BorrowStatus b = opt.get();

        if (body.get("amountPaise") != null) {
            try {
                int amountPaise = Integer.parseInt(body.get("amountPaise").toString());
                int rupees = Math.max(0, amountPaise / 100);
                if (b.getFineAmount() == null || b.getFineAmount() == 0) {
                    b.setFineAmount(rupees);
                }
            } catch (Exception ignored) {}
        }

        b.setFinePaid(1);
        try {
            Object p = body.get("paymentId");
            if (p != null) b.setFinePaymentId(p.toString());
        } catch (Exception ignored) {}

        borrowRepo.save(b);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Fine marked as paid"
        ));
    }

    /* ---------- CASH (admin) ---------- */

    @PostMapping("/cash")
    public ResponseEntity<?> cash(@RequestBody Map<String, Object> body) {
        if (body == null || !body.containsKey("borrowId")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "borrowId is required"
            ));
        }
        int borrowId = Integer.parseInt(body.get("borrowId").toString());
        Optional<BorrowStatus> opt = borrowRepo.findById(borrowId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "error",
                    "message", "Borrow not found"
            ));
        }
        BorrowStatus b = opt.get();

        int amount = 0;
        if (body.get("amount") != null) {
            try {
                amount = Math.max(0, Integer.parseInt(body.get("amount").toString()));
            } catch (Exception ignored) {}
        }

        if ((b.getFineAmount() == null || b.getFineAmount() == 0) && amount > 0) {
            b.setFineAmount(amount);
        }

        b.setFinePaid(1);
        try { b.setFinePaymentId("CASH-" + System.currentTimeMillis()); } catch (Exception ignored) {}

        borrowRepo.save(b);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Fine recorded as cash paid"
        ));
    }

    /** Path-variable version used by admin JS: POST /api/payments/cash/{borrowId} */
    @PostMapping("/cash/{borrowId}")
    public ResponseEntity<?> cashWithPath(
            @PathVariable int borrowId,
            @RequestBody(required = false) Map<String, Object> body) {

        Map<String, Object> merged = (body == null) ? new HashMap<>() : new HashMap<>(body);
        merged.put("borrowId", borrowId);
        return cash(merged);
    }

    /* ---------- LOST handling (admin) ---------- */

    @PostMapping("/lost/{borrowId}")
    public ResponseEntity<?> markLost(
            @PathVariable int borrowId,
            @RequestBody(required = false) Map<String, Object> body) {

        Optional<BorrowStatus> opt = borrowRepo.findById(borrowId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "error",
                    "message", "Borrow not found"
            ));
        }
        BorrowStatus b = opt.get();

        int addAmount = 0;
        String mode = "ADD_ONLY";
        boolean markPaid = false;

        if (body != null) {
            if (body.get("amount") != null) {
                try {
                    addAmount = Math.max(0, Integer.parseInt(body.get("amount").toString()));
                } catch (Exception ignored) {}
            }
            if (body.get("mode") != null) {
                mode = body.get("mode").toString().toUpperCase();
            }
            if (body.get("markPaid") != null) {
                markPaid = Boolean.parseBoolean(body.get("markPaid").toString());
            } else {
                markPaid = "CASH".equals(mode) || "RAZORPAY".equals(mode);
            }
        }

        int currentFine = (b.getFineAmount() == null ? 0 : b.getFineAmount());
        int newFine = currentFine + addAmount;
        b.setFineAmount(newFine);

        if (markPaid) {
            b.setFinePaid(1);
            try {
                if ("CASH".equals(mode)) {
                    b.setFinePaymentId("CASH-LOST-" + System.currentTimeMillis());
                } else if ("RAZORPAY".equals(mode)) {
                    b.setFinePaymentId("RZP-LOST-" + System.currentTimeMillis());
                }
            } catch (Exception ignored) {}
        }

        borrowRepo.save(b);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Lost amount added",
                "fineAmount", newFine,
                "finePaid", b.getFinePaid()
        ));
    }

    /* ================= Student history endpoints ================= */

    @GetMapping("/by-email")
    public ResponseEntity<?> byEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(
                paymentRepository
                        .findByPayerEmailIgnoreCaseOrderByCreatedAtDesc(email)
                        .stream()
                        .map(p -> Map.of(
                                "createdAt",  p.getCreatedAt(),
                                "capturedAt", p.getCapturedAt(),
                                "amount",     p.getAmount(),
                                "orderId",    p.getOrderId(),
                                "paymentId",  p.getPaymentId(),
                                "status",     p.getStatus(),
                                "method",     p.getMethod(),
                                "note",       p.getNote()
                        ))
                        .toList()
        );
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestParam("email") String email) {
        return byEmail(email);
    }

    /* ================= Helpers ================= */

    /** Computes live overdue fine (₹1/day). */
    private int computeLiveFine(BorrowStatus b) {
        if (Boolean.TRUE.equals(b.isReturned())) return 0;
        LocalDate expected = b.getExpectedReturnDate();
        if (expected == null) return 0;
        LocalDate today = LocalDate.now();
        if (!today.isAfter(expected)) return 0;
        long daysLate = ChronoUnit.DAYS.between(expected, today);
        return (int) Math.max(0, daysLate);
    }

    /**
     * Helper: if notes is null or empty, create a Map with a description & borrowId.
     * This avoids using String.isBlank() and matches your DTO (Map<String,Object>).
     */
    private void ensureDefaultNotes(CreateOrderRequest req, String prefix) {
        if (req.getBorrowId() == null) return;

        Map<String, Object> notes = req.getNotes();
        if (notes == null || notes.isEmpty()) {
            notes = new HashMap<>();
            notes.put("description", prefix + " " + req.getBorrowId());
            notes.put("borrowId", req.getBorrowId());
            req.setNotes(notes);
        }
    }
}
