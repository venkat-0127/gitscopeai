package com.lms.Digital_library.service;

import com.lms.Digital_library.dto.CreateOrderRequest;
import com.lms.Digital_library.dto.CreateOrderResponse;
import com.lms.Digital_library.dto.RefundRequest;
import com.lms.Digital_library.dto.VerifyPaymentRequest;
import com.lms.Digital_library.entity.Payment;
import com.lms.Digital_library.util.HmacSigner;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RazorpayService {

    private final PaymentService paymentService;

    // do NOT make this final; we will lazily construct if Spring didn't inject a bean
    private RazorpayClient client;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    /* ---------- Ensure we have a client ---------- */
    private RazorpayClient getClient() throws Exception {
        if (client == null) {
            client = new RazorpayClient(keyId, keySecret);
        }
        return client;
    }

    private static long rupeesToPaise(BigDecimal rupees) {
        if (rupees == null) return 0L;
        return rupees.movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /* ---------- Create Order ---------- */
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest req) throws Exception {
        // 1) Create a pending payment skeleton
        Payment p = paymentService.createSkeleton(
                req.getAmountRupees(),
                req.getPayerEmail(),
                req.getBorrowId(),
                req.getReceipt(),
                "RAZORPAY"
        );

        // 2) Build payload in PAISE
        long amountPaise = rupeesToPaise(req.getAmountRupees());

        JSONObject payload = new JSONObject();
        payload.put("amount", amountPaise);
        payload.put("currency", "INR");

        String receipt = (req.getReceipt() == null || req.getReceipt().isBlank())
                ? ("lms_rcpt_" + p.getId())
                : req.getReceipt();
        payload.put("receipt", receipt);
        payload.put("payment_capture", 1);

        if (req.getNotes() != null && !req.getNotes().isEmpty()) {
            payload.put("notes", new JSONObject(req.getNotes()));
        }

        // ✅ your SDK uses LOWERCASE accessors
        Order order = getClient().orders.create(payload);
        String orderId = order.get("id");

        // 3) attach order id to our payment row
        paymentService.attachOrderId(p, orderId);

        // 4) return to UI
        return new CreateOrderResponse(orderId, keyId, amountPaise, "INR", receipt);
    }

    /* ---------- Verify Signature (student flow) ---------- */
    @Transactional
    public boolean verifySignature(VerifyPaymentRequest req) {
        if (req.getRazorpayOrderId() == null ||
            req.getRazorpayPaymentId() == null ||
            req.getRazorpaySignature() == null) {
            return false;
        }

        String data = req.getRazorpayOrderId() + "|" + req.getRazorpayPaymentId();
        String expected = HmacSigner.hmacSha256Hex(keySecret, data);

        boolean ok = HmacSigner.equalsConstantTime(
                expected,
                req.getRazorpaySignature().trim()
        );

        if (ok) {
            // mark PENDING row as having a verified payment id (finalization happens in controller)
            paymentService.markVerified(req.getRazorpayOrderId(), req.getRazorpayPaymentId());
        }

        return ok;
    }

    /* ---------- Refund (optional) ---------- */
    @Transactional
    public String refund(RefundRequest req) throws Exception {
        JSONObject payload = new JSONObject();
        Long amountPaise = req.getAmountPaise();
        if (amountPaise != null && amountPaise > 0) {
            payload.put("amount", amountPaise);
        }

        // ✅ your SDK uses LOWERCASE accessors
        Refund refund = getClient().payments.refund(req.getRazorpayPaymentId(), payload);
        return refund.get("id");
    }
}
