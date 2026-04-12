package com.lms.Digital_library.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // 📤 Send OTP Email
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

    // ✅ Send Borrow Approval Email
    public void sendBorrowApprovedEmail(String toEmail, String bookTitle, String bookCode,
                                        LocalDate issueDate, LocalDate returnDate, String pdfFileName) {
        try {
            String userType = (toEmail != null && toEmail.startsWith("22")) ? "Student" : "Faculty";
            String pdfUrl = "http://localhost:8083/api/books/pdf/" + bookCode;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("📘 Book Approved: " + bookTitle);

            String body = "Dear " + userType + ",\n\n"
                    + "Your request for the book \"" + bookTitle + "\" has been approved.\n\n"
                    + "🔖 Book Code: " + bookCode + "\n"
                    + "📅 Issue Date: " + issueDate + "\n"
                    + "📆 Expected Return: " + returnDate + "\n"
                    + "🔗 PDF Link: " + pdfUrl + "\n\n"
                    + "📌 Please return the book within 15 days to avoid fines (₹1/day for students).\n\n"
                    + "Thank you,\n— LMS Admin\nLingaya’s Institute";

            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ NEW: Send Return Approval Email
    public void sendReturnApprovedEmail(String toEmail, String bookTitle, String bookCode,
                                        int fineAmount, boolean finePaid) {
        try {
            String subject = "📕 Book Return Approved: " + bookTitle;

            StringBuilder body = new StringBuilder();
            body.append("Dear User,\n\n");
            body.append("Your return request for the book has been approved successfully.\n\n");
            body.append("📖 Book Title: ").append(bookTitle).append("\n");
            body.append("🔖 Book Code: ").append(bookCode).append("\n");
            body.append("💰 Fine Amount: ₹").append(fineAmount).append("\n");
            body.append("✅ Fine Paid: ").append(finePaid ? "Yes" : "No").append("\n\n");

            if (finePaid) {
                body.append("📄 Invoice has been generated and attached in your LMS account.\n\n");
            } else if (fineAmount > 0) {
                body.append("⚠️ Your fine is pending. Please pay from your LMS account to clear the dues.\n\n");
            }

            body.append("Thank you for using our Library Management System!\n\n");
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

    // ✅ NEW: Generic email sender (reusable)
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

    // ✅ NEW: Payment confirmation email (when fine is paid)
    public void sendPaymentReceivedEmail(String toEmail, String bookTitle, int borrowId, int fineAmount) {
        try {
            String subject = "Payment Received - LMS (Borrow ID: " + borrowId + ")";
            StringBuilder body = new StringBuilder();
            body.append("Dear User,\n\n");
            body.append("We have received your fine payment for the following book:\n\n");
            body.append("📖 Title: ").append(bookTitle).append("\n");
            body.append("🆔 Borrow ID: ").append(borrowId).append("\n");
            body.append("💰 Amount: ₹").append(fineAmount).append("\n\n");
            body.append("Your payment is successful and the library record has been updated.\n\n");
            body.append("Thank you,\n— LMS Team");

            sendGenericEmail(toEmail, subject, body.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
