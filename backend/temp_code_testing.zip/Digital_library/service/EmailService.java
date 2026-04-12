package com.lms.Digital_library.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /* =========================================================
       OTP
    ========================================================= */
    public void sendOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("🔐 Your LMS OTP");

            String body = "Dear User,\n\n"
                    + "Thank you for using the Library Management System of Lingaya’s Institute.\n\n"
                    + "Your One-Time Password (OTP) is: " + otp + "\n\n"
                    + "⏳ This OTP is valid for 5 minutes.\n"
                    + "❗ Please do not share this code with anyone.\n\n"
                    + "📚 Happy Reading!\n"
                    + "— LMS Team\nLingaya’s Institute of Management & Technology";

            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================================================
       Borrow approved
    ========================================================= */
    public void sendBorrowApprovedEmail(String toEmail, String bookTitle, String bookCode,
                                        LocalDate issueDate, LocalDate returnDate, String pdfPathOrId) {
        try {
            String userType = (toEmail != null && toEmail.startsWith("22")) ? "Student" : "Faculty";

            // Try to build a sensible PDF link. If a direct path is provided, use it; else fall back to code.
            String pdfUrl;
            if (pdfPathOrId != null && !pdfPathOrId.isBlank()) {
                // If you expose /api/books/pdf/{id} then pass id as pdfPathOrId. Otherwise, if it's an actual URL/path, this still looks fine.
                pdfUrl = pdfPathOrId.startsWith("http") || pdfPathOrId.startsWith("/")
                        ? pdfPathOrId
                        : ("/api/books/pdf/" + pdfPathOrId);
            } else {
                pdfUrl = "/api/books/pdf/" + bookCode;
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("📘 Book Approved: " + bookTitle);

            String body = "Dear " + userType + ",\n\n"
                    + "Your request for the book \"" + bookTitle + "\" has been approved.\n\n"
                    + "🔖 Book Code: " + bookCode + "\n"
                    + "📅 Issue Date: " + (issueDate != null ? issueDate.format(fmt) : "-") + "\n"
                    + "📆 Expected Return: " + (returnDate != null ? returnDate.format(fmt) : "-") + "\n"
                    + "🔗 PDF Link: " + pdfUrl + "\n\n"
                    + "📌 Please return the book within 15 days to avoid fines (₹1/day for students).\n\n"
                    + "Thank you,\n— LMS Admin\nLingaya’s Institute";

            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================================================
       Return approved (BACKWARD-COMPATIBLE signature)
       - Calls the richer version below with defaults.
    ========================================================= */
    public void sendReturnApprovedEmail(String toEmail, String bookTitle, String bookCode,
                                        int fineAmount, boolean finePaid) {
        // Keep old calls working by routing to the new rich email with defaults
        String method = finePaid ? "Razorpay" : "Unpaid";
        String ref    = finePaid ? "-" : "-";
        sendReturnApprovedEmail(toEmail, bookTitle, bookCode, fineAmount, finePaid, method, ref);
    }

    /* =========================================================
       Return approved (RICH RECEIPT)
       - Includes payment method (Razorpay/Cash/Unpaid) and reference id
    ========================================================= */
    public void sendReturnApprovedEmail(String toEmail,
                                        String bookTitle,
                                        String bookCode,
                                        int fineAmount,
                                        boolean finePaid,
                                        String paymentMethod,
                                        String paymentRef) {
        try {
            String subject = "📕 Book Return Approved — Receipt: " + safe(bookTitle);

            StringBuilder body = new StringBuilder();
            body.append("Dear User,\n\n");
            body.append("Your book return has been approved.\n\n");
            body.append("📖 Book Title : ").append(safe(bookTitle)).append("\n");
            body.append("🔖 Book Code  : ").append(safe(bookCode)).append("\n");
            body.append("💰 Fine Amount: ₹").append(Math.max(0, fineAmount)).append("\n");
            body.append("✅ Status     : ").append(finePaid ? "Paid" : "Unpaid").append("\n");

            if (finePaid) {
                body.append("💳 Paid By    : ").append(safe(paymentMethod == null ? "Razorpay" : paymentMethod)).append("\n");
                body.append("🧾 Reference  : ").append(safe(paymentRef == null ? "-" : paymentRef)).append("\n");
            } else if (fineAmount > 0) {
                body.append("\n⚠️ Your fine is pending. Please complete the payment in your LMS account.\n");
            }

            body.append("\nThank you for using our Library Management System!\n\n");
            body.append("Regards,\nLibrary Team");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body.toString());

            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================================================
       Generic email (reusable)
    ========================================================= */
    public void sendGenericEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================================================
       Payment received (when fine is paid)
    ========================================================= */
    public void sendPaymentReceivedEmail(String toEmail, String bookTitle, int borrowId, int fineAmount) {
        try {
            String subject = "Payment Received - LMS (Borrow ID: " + borrowId + ")";
            StringBuilder body = new StringBuilder();
            body.append("Dear User,\n\n");
            body.append("We have received your fine payment for the following book:\n\n");
            body.append("📖 Title : ").append(safe(bookTitle)).append("\n");
            body.append("🆔 Borrow ID: ").append(borrowId).append("\n");
            body.append("💰 Amount: ₹").append(Math.max(0, fineAmount)).append("\n\n");
            body.append("Your payment is successful and the library record has been updated.\n\n");
            body.append("Thank you,\n— LMS Team");

            sendGenericEmail(toEmail, subject, body.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================================================
       Helpers
    ========================================================= */
    private String safe(String s) {
        return (s == null) ? "-" : s;
    }
}
