package com.lms.Digital_library.controller;

import com.lms.Digital_library.service.PaymentService;
import com.lms.Digital_library.util.HmacSigner;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final PaymentService paymentService;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    private static final String SIGNATURE_HEADER = "X-Razorpay-Signature";

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            HttpServletRequest request,
            @RequestHeader(name = SIGNATURE_HEADER, required = false) String signatureHeader
    ) throws Exception {
        String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        if (signatureHeader == null) return ResponseEntity.badRequest().body("Missing signature");

        String expected = HmacSigner.hmacSha256Hex(webhookSecret, body);
        if (!HmacSigner.equalsConstantTime(expected, signatureHeader)) {
            return ResponseEntity.status(400).body("Invalid signature");
        }

        JSONObject evt = new JSONObject(body);
        String eventType = evt.optString("event", "");
        JSONObject payload = evt.optJSONObject("payload");

        String paymentId = null, orderId = null, method = null, errorCode = null, errorDesc = null;

        if (payload != null && payload.has("payment")) {
            JSONObject entity = payload.getJSONObject("payment").optJSONObject("entity");
            if (entity != null) {
                paymentId = entity.optString("id", null);
                orderId   = entity.optString("order_id", null);
                method    = entity.optString("method", null);
                JSONObject error = entity.optJSONObject("error");
                if (error != null) {
                    errorCode = error.optString("code", null);
                    errorDesc = error.optString("description", null);
                }
            }
        } else if (payload != null && payload.has("order")) {
            JSONObject entity = payload.getJSONObject("order").optJSONObject("entity");
            if (entity != null) orderId = entity.optString("id", null);
        }

        switch (eventType) {
            case "payment.captured":
            case "order.paid":
                if (paymentId != null) paymentService.markPaid(paymentId, method);
                break;
            case "payment.failed":
                paymentService.markFailed(orderId, paymentId, errorCode, errorDesc);
                break;
            default:
                // log/ignore others
        }
        return ResponseEntity.ok("OK");
    }
}
