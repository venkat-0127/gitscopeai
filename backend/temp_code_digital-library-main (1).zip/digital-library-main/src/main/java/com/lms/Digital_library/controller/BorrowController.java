package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.Book;
import com.lms.Digital_library.entity.BorrowStatus;
import com.lms.Digital_library.repository.BookRepository;
import com.lms.Digital_library.repository.BorrowStatusRepository;
import com.lms.Digital_library.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/borrow")
@CrossOrigin
public class BorrowController {

    @Autowired
    private BorrowStatusRepository borrowRepo;

    @Autowired
    private BookRepository bookRepo;

    @Autowired
    private EmailService emailService;

    // 📥 Borrow Book (student request)
    @PostMapping("/request")
    public Map<String, Object> requestBorrow(@RequestParam String email, @RequestParam int bookId) {
        Optional<Book> bookOpt = bookRepo.findById(bookId);
        Map<String, Object> res = new HashMap<>();

        if (bookOpt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Book not found.");
            return res;
        }

        Book book = bookOpt.get();
        if (book.getAvailable() <= 0) {
            res.put("status", "error");
            res.put("message", "No copies available.");
            return res;
        }

        BorrowStatus b = new BorrowStatus();
        b.setUserEmail(email);
        b.setBookId(bookId);
        b.setBookTitle(book.getTitle());
        b.setRequestDate(LocalDate.now());
        b.setStatus("PENDING");
        b.setReturned(false);
        // default fine fields
        b.setFineAmount(0);
        b.setFinePaid(false);

        borrowRepo.save(b);
        res.put("status", "success");
        res.put("message", "Borrow request sent.");
        return res;
    }

    // NOTE: older direct-return endpoint (kept for compatibility).
    // Prefer admin approval flow (/api/admin/approve-return/{id}).
    // 📤 Return Book (direct) — uses fineAmount / finePaid fields
    @PostMapping("/return/{id}")
    public Map<String, Object> returnBook(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        Optional<BorrowStatus> opt = borrowRepo.findById(id);

        if (opt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Borrow record not found.");
            return res;
        }

        BorrowStatus b = opt.get();
        if (!"APPROVED".equals(b.getStatus())) {
            res.put("status", "error");
            res.put("message", "Book not approved or already returned.");
            return res;
        }

        // compute fine (students only; student email starts with "22")
        int fineAmount = 0;
        if (b.getExpectedReturnDate() != null && b.getUserEmail() != null && b.getUserEmail().startsWith("22")) {
            LocalDate now = LocalDate.now();
            if (now.isAfter(b.getExpectedReturnDate())) {
                long overdue = b.getExpectedReturnDate().until(now).getDays();
                if (overdue > 0) fineAmount = (int) overdue;
            }
        }

        b.setReturned(true);
        b.setStatus("RETURNED");
        b.setReturnDate(LocalDate.now());
        b.setFineAmount(fineAmount);
        // finePaid remains as-is (likely false) — in real flow payment happens separately
        borrowRepo.save(b);

        // Increase book stock
        Optional<Book> bookOpt = bookRepo.findById(b.getBookId());
        bookOpt.ifPresent(book -> {
            book.setAvailable(book.getAvailable() + 1);
            bookRepo.save(book);
        });

        res.put("status", "returned");
        res.put("fineAmount", b.getFineAmount());
        res.put("finePaid", b.isFinePaid());
        return res;
    }

    // 📥 Student: request a return (this sets status = PENDING_RETURN)
    @PostMapping("/request-return/{id}")
    public Map<String, Object> requestReturn(@PathVariable int id, @RequestParam(required = false) String note) {
        Map<String, Object> res = new HashMap<>();
        Optional<BorrowStatus> opt = borrowRepo.findById(id);

        if (opt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Borrow record not found.");
            return res;
        }

        BorrowStatus b = opt.get();
        if (!"APPROVED".equals(b.getStatus())) {
            res.put("status", "error");
            res.put("message", "Cannot request return. Current status: " + b.getStatus());
            return res;
        }

        b.setStatus("PENDING_RETURN");
        // optionally store student note if you add a field later
        borrowRepo.save(b);

        // notify admin (simple email)
        try {
            String adminEmail = "venkatthota9381@gmail.com"; // change if you have config
            String subject = "Return Requested: " + b.getBookTitle();
            String body = "User " + b.getUserEmail() + " has requested to return book '" + b.getBookTitle() +
                    "' (Borrow ID: " + b.getId() + "). Please review in admin panel.";
            emailService.sendGenericEmail(adminEmail, subject, body);
        } catch (Exception ex) {
            // don't block user flow if email fails
            ex.printStackTrace();
        }

        res.put("status", "requested");
        res.put("message", "Return request submitted. Admin will approve it.");
        return res;
    }

    // 📤 Simulated payment endpoint (mark fine as paid) — will be replaced by Razorpay flow later
    @PostMapping("/pay-fine/{id}")
    public Map<String, Object> payFine(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        Optional<BorrowStatus> opt = borrowRepo.findById(id);

        if (opt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Borrow record not found.");
            return res;
        }

        BorrowStatus b = opt.get();
        if (b.getFineAmount() <= 0) {
            res.put("status", "error");
            res.put("message", "No fine to pay.");
            return res;
        }

        b.setFinePaid(true);
        borrowRepo.save(b);

        // send payment confirmation email to student
        try {
            emailService.sendPaymentReceivedEmail(
                    b.getUserEmail(),
                    b.getBookTitle(),
                    b.getId(),
                    b.getFineAmount()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        res.put("status", "success");
        res.put("message", "Fine paid successfully (simulated).");
        res.put("fineAmount", b.getFineAmount());
        return res;
    }

    // 📥 Borrowed books by user (student/teacher)
    @GetMapping("/my-books")
    public List<BorrowStatus> getMyBooks(@RequestParam String email) {
        return borrowRepo.findByUserEmailOrderByIdDesc(email);
    }

    // 📋 All borrowed records (admin)
    @GetMapping("/all-borrowed")
    public List<BorrowStatus> getAllBorrowedBooks() {
        return borrowRepo.findAll();
    }
}
