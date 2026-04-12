package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.Book;
import com.lms.Digital_library.entity.BorrowStatus;
import com.lms.Digital_library.repository.BookRepository;
import com.lms.Digital_library.repository.BorrowStatusRepository;
import com.lms.Digital_library.service.AdminService;
import com.lms.Digital_library.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private BorrowStatusRepository borrowRepo;

    @Autowired
    private BookRepository bookRepo;

    @Autowired
    private EmailService emailService;

    // ✅ Admin login
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> creds) {
        String username = creds.get("username");
        String password = creds.get("password");

        Map<String, Object> response = new HashMap<>();
        if (username == null || password == null) {
            response.put("status", "error");
            response.put("message", "Username and password required");
            return response;
        }

        if (adminService.login(username, password)) {
            response.put("status", "success");
            response.put("message", "Login successful");
            response.put("role", "admin");
        } else {
            response.put("status", "error");
            response.put("message", "Invalid credentials");
        }

        return response;
    }

    // ✅ Approve a borrow request
    @PostMapping("/approve/{id}")
    public Map<String, Object> approveBorrow(@PathVariable int id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        Optional<BorrowStatus> opt = borrowRepo.findById(id);
        if (opt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Borrow request not found.");
            return response;
        }

        BorrowStatus borrow = opt.get();
        if (!"PENDING".equals(borrow.getStatus())) {
            response.put("status", "error");
            response.put("message", "Already approved or returned.");
            return response;
        }

        Optional<Book> bookOpt = bookRepo.findById(borrow.getBookId());
        if (bookOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Book not found.");
            return response;
        }

        String bookCode = body.get("bookCode");
        if (bookCode == null || bookCode.trim().isEmpty()) {
            response.put("status", "error");
            response.put("message", "Book code is required.");
            return response;
        }

        // Approve borrow
        borrow.setStatus("APPROVED");
        borrow.setBookCode(bookCode);
        borrow.setBorrowDate(LocalDate.now());
        // default student loan period = 15 days; you may extend for teacher later
        borrow.setExpectedReturnDate(LocalDate.now().plusDays(15));
        borrowRepo.save(borrow);

        Book book = bookOpt.get();
        if (book.getAvailable() <= 0) {
            response.put("status", "error");
            response.put("message", "No available copies left.");
            return response;
        }
        book.setAvailable(book.getAvailable() - 1);
        bookRepo.save(book);

        // send email to user notifying approval
        try {
            emailService.sendBorrowApprovedEmail(
                    borrow.getUserEmail(),
                    book.getTitle(),
                    bookCode,
                    borrow.getBorrowDate(),
                    borrow.getExpectedReturnDate(),
                    book.getPdfPath()
            );
        } catch (Exception e) {
            // don't fail the request if email has problems
            e.printStackTrace();
        }

        response.put("status", "approved");
        response.put("message", "Request approved and email sent.");
        return response;
    }

    // ✅ Approve a return (with fine calculation + email)
    @PostMapping("/approve-return/{id}")
    public Map<String, Object> approveReturn(@PathVariable int id, @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        Optional<BorrowStatus> opt = borrowRepo.findById(id);

        if (opt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Borrow record not found.");
            return response;
        }

        BorrowStatus borrow = opt.get();

        // Allow admin to approve if student requested return (PENDING_RETURN) or admin directly processes (APPROVED)
        if (!"PENDING_RETURN".equals(borrow.getStatus()) && !"APPROVED".equals(borrow.getStatus())) {
            response.put("status", "error");
            response.put("message", "Return cannot be approved. Current status: " + borrow.getStatus());
            return response;
        }

        Optional<Book> bookOpt = bookRepo.findById(borrow.getBookId());
        if (bookOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Book not found.");
            return response;
        }

        // Calculate late days safely (handle null expectedReturnDate)
        LocalDate today = LocalDate.now();
        long daysLate = 0;
        if (borrow.getExpectedReturnDate() != null) {
            if (today.isAfter(borrow.getExpectedReturnDate())) {
                daysLate = ChronoUnit.DAYS.between(borrow.getExpectedReturnDate(), today);
            }
        }

        // Role-based fine logic - student emails start with 22...@limat.edu.in (pattern from your system)
        String userEmail = (borrow.getUserEmail() != null) ? borrow.getUserEmail().toLowerCase() : "";
        boolean isStudent = userEmail.matches(".*\\d{2}[a-z]{2}\\d{4}.*@limat\\.edu\\.in");
        int fineAmount = 0;
        if (isStudent && daysLate > 0) {
            fineAmount = (int) daysLate; // ₹1 per day after due date
        }

        // Safe boolean parsing for finePaid from request body
        boolean finePaid = false;
        if (body != null && body.get("finePaid") != null) {
            Object fineObj = body.get("finePaid");
            if (fineObj instanceof Boolean) {
                finePaid = (Boolean) fineObj;
            } else {
                finePaid = Boolean.parseBoolean(String.valueOf(fineObj));
            }
        }

        // Update borrow record
        borrow.setStatus("RETURNED");
        borrow.setReturned(true);
        borrow.setReturnDate(today);
        borrow.setFineAmount(fineAmount);
        borrow.setFinePaid(finePaid);
        borrowRepo.save(borrow);

        // Update book availability (+1)
        Book book = bookOpt.get();
        book.setAvailable(book.getAvailable() + 1);
        bookRepo.save(book);

        // Send return confirmation email (includes fine & payment status)
        try {
            emailService.sendReturnApprovedEmail(
                    borrow.getUserEmail(),
                    book.getTitle(),
                    borrow.getBookCode(),
                    fineAmount,
                    finePaid
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Response
        response.put("status", "success");
        response.put("message", "Book return approved.");
        response.put("fineAmount", fineAmount);
        response.put("finePaid", finePaid);
        return response;
    }

    // 📥 Get all pending requests
    @GetMapping("/pending-requests")
    public List<BorrowStatus> getPendingRequests() {
        return borrowRepo.findByStatus("PENDING");
    }

    // 📋 View all borrowed records
    @GetMapping("/borrowed-list")
    public List<BorrowStatus> getAllBorrowed() {
        return borrowRepo.findAll();
    }

    // 📋 View borrowed records by email (filter)
    @GetMapping("/borrowed-list/by-email")
    public List<BorrowStatus> getBorrowedByEmail(@RequestParam String email) {
        return borrowRepo.findByUserEmailContainingIgnoreCase(email);
    }

    // 👥 View registered users
    @GetMapping("/registered-users")
    public Set<String> getRegisteredUsers() {
        List<BorrowStatus> all = borrowRepo.findAll();
        Set<String> users = new HashSet<>();
        for (BorrowStatus b : all) {
            users.add(b.getUserEmail());
        }
        return users;
    }
}
