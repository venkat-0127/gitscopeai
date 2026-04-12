// File: src/main/java/com/lms/Digital_library/controller/CertificateController.java
package com.lms.Digital_library.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lms.Digital_library.entity.BorrowStatus;
import com.lms.Digital_library.entity.Payment;
import com.lms.Digital_library.repository.BorrowStatusRepository;
import com.lms.Digital_library.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@RestController
@RequestMapping("/api/cert")
@RequiredArgsConstructor
@CrossOrigin
public class CertificateController {

    private final BorrowStatusRepository borrowRepo;
    private final PaymentRepository paymentRepo;

    /** Change this in prod */
    private static final String NO_DUE_SECRET = "lms_secret_change_me";

    /*
     * -----------------------------------------------------------
     * NO-DUE CERTIFICATE (PNG)
     * -----------------------------------------------------------
     * RULE: Only issue if the user has NO active/pending/overdue items,
     * and NO unpaid fines in ANY borrow record.
     * Otherwise respond 400 with a clear message.
     * -----------------------------------------------------------
     */
    @GetMapping(value = "/no-due", produces = MediaType.IMAGE_PNG_VALUE)
    public void noDueImage(@RequestParam("email") String email,
            HttpServletResponse resp) throws IOException {

        // Fetch all records for the user (case-insensitive).
        List<BorrowStatus> records = borrowRepo.findByUserEmailContainingIgnoreCase(email);

        if (records == null || records.isEmpty()) {
            resp.sendError(400, "No borrowing records found for this user.");
            return;
        }

        // Global eligibility check across ALL records
        LocalDate today = LocalDate.now();
        boolean hasBlockingItem = records.stream().anyMatch(b -> {
            String status = String.valueOf(b.getStatus()).toUpperCase(Locale.ROOT);
            boolean returned = b.isReturned();

            // Pending/active states that block no-due
            boolean pendingState = status.equals("REQUESTED")
                    || status.equals("PENDING")
                    || status.equals("APPROVED")
                    || status.equals("PENDING_RETURN")
                    || status.equals("RETURN_REQUESTED");

            // Overdue if today is after expected date AND not returned
            boolean overdue = b.getExpectedReturnDate() != null
                    && today.isAfter(b.getExpectedReturnDate())
                    && !returned;

            // Unpaid fine
            int fineAmt = Optional.ofNullable(b.getFineAmount()).orElse(0);
            boolean fineUnpaid = fineAmt > 0 && (b.getFinePaid() == null || b.getFinePaid() == 0);

            // Any of these conditions blocks no-due
            return pendingState || overdue || !returned || fineUnpaid;
        });

        if (hasBlockingItem) {
            resp.sendError(400,
                    "❌ No-Due cannot be issued — You have pending returns, overdue items, or unpaid fines.");
            return;
        }

        // Choose the most recent CLEAR record (returned & fine cleared)
        BorrowStatus lastCleared = records.stream()
                .filter(BorrowStatus::isReturned)
                .filter(b -> Optional.ofNullable(b.getFineAmount()).orElse(0) <= 0
                        || (b.getFinePaid() != null && b.getFinePaid() != 0))
                .max(Comparator.comparingInt(BorrowStatus::getId))
                .orElse(records.get(0)); // Should exist because we passed the global check

        // Verification code ties to email + today
        String payload = email + "|" + LocalDate.now() + "|" + NO_DUE_SECRET;
        String code = DigestUtils.md5DigestAsHex(payload.getBytes(StandardCharsets.UTF_8))
                .substring(0, 10).toUpperCase(Locale.ROOT);

        // Prefer the last return date if present, otherwise today
        LocalDate issuedOn = Optional.ofNullable(lastCleared.getReturnDate()).orElse(LocalDate.now());

        BufferedImage img = renderCard(
                "Library No-Due Certificate",
                new String[] {
                        "Issued To : " + safe(lastCleared.getUserEmail()),
                        "Book      : " + safe(lastCleared.getBookTitle()),
                        "Book Code : " + safe(lastCleared.getBookCode()),
                        "Issued On : " + issuedOn.format(DateTimeFormatter.ISO_DATE),
                        "Status    : CLEARED"
                },
                "Verification Code: " + code,
                "verify:no-due|" + code + "|" + email);

        // Optional: make the image non-cacheable if you wish
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setContentType(MediaType.IMAGE_PNG_VALUE);
        ImageIO.write(img, "png", resp.getOutputStream());
    }

    /*
     * -----------------------------------------------------------
     * PAYMENT RECEIPT (PNG)
     * -----------------------------------------------------------
     */
    @GetMapping(value = "/payment-receipt", produces = MediaType.IMAGE_PNG_VALUE)
    public void paymentReceipt(@RequestParam("paymentId") String paymentId,
            HttpServletResponse resp) throws IOException {
        Optional<Payment> opt = paymentRepo.findByPaymentId(paymentId);
        if (opt.isEmpty()) {
            resp.sendError(404, "Payment not found");
            return;
        }

        Payment p = opt.get();
        String email = safe(p.getPayerEmail());
        BigDecimal amount = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount();
        String method = safe(p.getMethod());
        String orderId = safe(p.getOrderId());
        String pid = safe(p.getPaymentId());
        String status = safe(p.getStatus());

        Instant when = p.getCapturedAt() != null
                ? p.getCapturedAt()
                : (p.getCreatedAt() != null
                        ? p.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
                        : Instant.now());

        String dateStr = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z")
                .withZone(ZoneId.systemDefault())
                .format(when);

        String qrPayload = "pay:" + pid + "|order:" + orderId + "|amt:" + amount + "|status:" + status;

        BufferedImage img = renderCard(
                "Payment Receipt",
                new String[] {
                        "Amount    : ₹" + amount.setScale(2),
                        "Status    : " + status.toUpperCase(Locale.ROOT),
                        "Payment Id: " + pid,
                        "Method    : " + method,
                        "Email     : " + email,
                        "Paid On   : " + dateStr
                },
                "Order Id  : " + orderId,
                qrPayload);

        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setContentType(MediaType.IMAGE_PNG_VALUE);
        ImageIO.write(img, "png", resp.getOutputStream());
    }

    /*
     * -----------------------------------------------------------
     * Card renderer (shared)
     * -----------------------------------------------------------
     */
    private static BufferedImage renderCard(String title, String[] lines, String footer, String qrText) {
        int w = 1200, h = 720, pad = 60;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Background + card
        g.setColor(new Color(248, 244, 235));
        g.fillRect(0, 0, w, h);
        g.setColor(Color.WHITE);
        g.fillRoundRect(pad, pad, w - 2 * pad, h - 2 * pad, 30, 30);
        g.setColor(new Color(230, 230, 230));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(pad, pad, w - 2 * pad, h - 2 * pad, 30, 30);

        // Text antialiasing
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Title
        g.setColor(new Color(33, 37, 41));
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        g.drawString(title, pad + 50, pad + 90);

        // Green tick
        g.setColor(new Color(40, 167, 69));
        g.fillOval(pad + 50, pad + 120, 80, 80);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(pad + 75, pad + 170, pad + 95, pad + 190);
        g.drawLine(pad + 95, pad + 190, pad + 130, pad + 145);

        // Info lines
        g.setColor(new Color(73, 80, 87));
        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        int y = pad + 160, x = pad + 160;
        for (String line : lines) {
            y += 50;
            g.drawString(line, x, y);
        }

        // Footer
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(new Color(52, 58, 64));
        g.drawString(footer, pad + 50, h - pad - 40);

        // QR code
        try {
            int q = 200;
            BufferedImage qr = makeQr(qrText, q, q);
            g.drawImage(qr, w - pad - q - 30, pad + 80, null);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g.drawString("Scan to verify", w - pad - q - 5, pad + 80 + q + 30);
        } catch (Exception ignored) {
        }

        // Watermark
        g.setColor(new Color(0, 0, 0, 25));
        g.setFont(new Font("SansSerif", Font.BOLD, 120));
        g.rotate(Math.toRadians(-25), w / 2.0, h / 2.0);
        g.drawString("LIBRARY LMS", w / 2 - 300, h / 2 + 40);
        g.dispose();
        return img;
    }

    private static BufferedImage makeQr(String text, int w, int h) throws WriterException {
        BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, w, h);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                img.setRGB(x, y, m.get(x, y) ? Color.BLACK.getRGB() : 0x00FFFFFF);
            }
        }
        return img;
    }

    private static String safe(String s) {
        return s == null ? "-" : s;
    }
}
