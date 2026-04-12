package com.lms.Digital_library.controller;

import com.lms.Digital_library.service.EmailService;
import com.lms.Digital_library.service.LoginRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin // allow from anywhere for dev; tighten in prod
public class OtpLoginController {

    // Thread-safe store: email -> (otp, expiresAtEpochSeconds)
    private static class OtpEntry {
        final String otp;
        final long expiresAt;
        OtpEntry(String otp, long expiresAt) { this.otp = otp; this.expiresAt = expiresAt; }
    }

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final long OTP_TTL_SECONDS = 10 * 60; // 10 minutes

    @Autowired(required = false)
    private EmailService emailService; // optional - may be null if not configured

    @Autowired
    private LoginRecordService loginRecordService;

    // Optional: if you want to force emailService presence in prod, remove required=false and handle startup.
    @PostConstruct
    public void init() {
        if (emailService == null) {
            System.out.println("Warning: EmailService is NOT configured. OTPs will be logged to console (mock mode).");
        }
    }

    // 1️⃣ Send OTP - accepts form-urlencoded (email=...) which matches your frontend
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestParam String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","email required"));
        }

        // Basic domain validation (optional) - keep lenient for demo
        String lower = email.trim().toLowerCase();
        if (!(lower.endsWith("@limat.edu.in") || lower.endsWith(".lingayas@limat.edu.in"))) {
            // allow through but warn — or uncomment to reject
            // return ResponseEntity.badRequest().body(Map.of("status","error","message","invalid domain"));
            System.out.println("Warning: sendOtp called with unusual domain: " + email);
        }

        // Generate OTP
        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);
        long expiresAt = Instant.now().getEpochSecond() + OTP_TTL_SECONDS;
        otpStore.put(lower, new OtpEntry(otp, expiresAt));

        // Try sending email; if it fails, catch and log -> fallback to mock
        boolean emailSent = false;
        if (emailService != null) {
            try {
                // Assuming your EmailService has method sendOtp(String email, String otp)
                emailService.sendOtp(email, otp);
                emailSent = true;
            } catch (Exception ex) {
                // don't let mail exceptions bubble up to produce 500
                System.err.println("EmailService.sendOtp threw an exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        // For demo / presentation: always log OTP (so you can copy from server console)
        System.out.println("=== OTP === for " + email + " => " + otp + " (valid for " + (OTP_TTL_SECONDS/60) + " minutes). Email sent? " + emailSent);

        // Respond success regardless of email success to avoid 500s in UI; include a message
        if (emailSent) {
            return ResponseEntity.ok(Map.of("status","success","message","OTP sent to your email"));
        } else {
            // In production you may want to return error; for demo, return success and tell you it's mock-logged.
            return ResponseEntity.ok(Map.of("status","success","message","OTP generated and logged on server (email not sent)"));
        }
    }

    // 2️⃣ Verify OTP
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","email and otp required"));
        }

        String lower = email.trim().toLowerCase();
        OtpEntry entry = otpStore.get(lower);
        if (entry == null) {
            return ResponseEntity.ok(Map.of("status","error","message","no otp found"));
        }

        long now = Instant.now().getEpochSecond();
        if (now > entry.expiresAt) {
            otpStore.remove(lower);
            return ResponseEntity.ok(Map.of("status","error","message","otp expired"));
        }

        if (!Objects.equals(entry.otp, otp.trim())) {
            return ResponseEntity.ok(Map.of("status","error","message","invalid otp"));
        }

        // success: remove otp and record login
        otpStore.remove(lower);

        String role = getRoleFromEmail(lower);
        try {
            loginRecordService.saveLogin(email, role);
        } catch (Exception ex) {
            // log but continue with success response
            System.err.println("Warning: loginRecordService.saveLogin threw: " + ex.getMessage());
            ex.printStackTrace();
        }

        return ResponseEntity.ok(Map.of("status","success","message","OTP verified","role",role));
    }

    // Basic role detection - keep your existing pattern (you can tweak)
    private String getRoleFromEmail(String email) {
        // your original pattern: ^\d{2}[a-zA-Z]{2}\d{1}[a-zA-Z]{1}\d{4}@limat.edu.in$
        // That regex looks very specific; for safety, treat emails with digits early as students
        if (email.matches("^\\d{2}[a-zA-Z]{2}\\d{1}[a-zA-Z]{1}\\d{4}@limat.edu.in$")) {
            return "student";
        } else {
            return "teacher";
        }
    }

    // Periodic cleanup - remove expired OTPs every 5 minutes
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void cleanupExpiredOtps() {
        long now = Instant.now().getEpochSecond();
        otpStore.entrySet().removeIf(e -> e.getValue().expiresAt < now);
    }
}
