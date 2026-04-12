package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.Admin;
import com.lms.Digital_library.entity.Book;
import com.lms.Digital_library.entity.BorrowStatus;
import com.lms.Digital_library.entity.Librarian;
import com.lms.Digital_library.repository.BookRepository;
import com.lms.Digital_library.repository.BorrowStatusRepository;
import com.lms.Digital_library.repository.LibrarianRepository;
import com.lms.Digital_library.service.AdminService;
import com.lms.Digital_library.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

/**
 * Unified AdminController:
 * - Auth (login)
 * - Admin management (create/list/deactivate/activate/reset/change-password)
 * - Borrow approval & return approval & no-due & lists (existing functionality)
 *
 * Note: Protect admin-management endpoints with Spring Security
 * (e.g. @PreAuthorize) in production.
 */
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

    // 🔹 NEW: to check librarian credentials when admin login fails
    @Autowired
    private LibrarianRepository librarianRepo;

    // 🔹 NEW: for password hash verification
    @Autowired
    private PasswordEncoder passwordEncoder;

    /* ===================== AUTH ===================== */

    /**
     * Login for admin or librarian.
     * Expects JSON { "username": "...", "password": "..." }
     *
     * - If username exists in ADMIN table and password is correct & active → return
     * admin data.
     * - Else, if username exists in LIBRARIANS table and password is correct &
     * active → return librarian data.
     * - Else → 401 Invalid credentials or account inactive.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> creds) {
        String username = creds.get("username");
        String password = creds.get("password");

        Map<String, Object> res = new HashMap<>();
        if (username == null || password == null) {
            res.put("status", "error");
            res.put("message", "Username and password required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
        }

        /* 1️⃣ TRY REAL ADMINS FIRST (SUPER_ADMIN, ADMIN, etc.) */
        Optional<Admin> adminOpt = adminService.login(username, password);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();

            res.put("status", "success");
            res.put("message", "Login successful");
            res.put("username", admin.getUsername());
            res.put("name", admin.getName());
            res.put("role", admin.getRole());
            res.put("accessModules", admin.getAccessModules()); // JSON string; frontend can parse
            res.put("mustChangePassword",
                    admin.getMustChangePassword() == null ? false : admin.getMustChangePassword());

            return ResponseEntity.ok(res);
        }

        /* 2️⃣ IF NOT AN ADMIN, TRY LIBRARIAN TABLE */
        Optional<Librarian> libOpt = librarianRepo.findByUsername(username);
        if (libOpt.isPresent()) {
            Librarian lib = libOpt.get();

            boolean active = lib.isActive(); // boolean field "active"
            String hash = lib.getPasswordHash(); // hashed password

            if (active && passwordEncoder.matches(password, hash)) {
                Map<String, Object> out = new HashMap<>();
                out.put("status", "success");
                out.put("message", "Login successful");
                out.put("username", lib.getUsername());
                out.put("name", lib.getUsername());
                out.put("role", "LIBRARIAN");
                // Keep it simple: librarian sees LMS module only
                out.put("accessModules", "[\"LMS\"]");
                out.put("mustChangePassword", false);
                return ResponseEntity.ok(out);
            }
        }

        /* 3️⃣ NEITHER ADMIN NOR LIBRARIAN MATCHED */
        res.put("status", "error");
        res.put("message", "Invalid credentials or account inactive");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
    }

    /* ===================== ADMIN MANAGEMENT ===================== */

    /**
     * Create a new admin (Super-Admin only).
     * POST /api/admin/create-admin
     * Body: { "username","password","name","role","modules": ["LMS","ARCHIVES"],
     * "mustChangePassword": true/false }
     *
     * Protect with @PreAuthorize("hasRole('SUPER_ADMIN')") in your security config.
     */
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, Object> body) {
        try {
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String name = (String) body.getOrDefault("name", "");
            String role = (String) body.getOrDefault("role", "ADMIN");
            boolean mustChange = body.getOrDefault("mustChangePassword", Boolean.TRUE) instanceof Boolean
                    ? (Boolean) body.getOrDefault("mustChangePassword", Boolean.TRUE)
                    : Boolean.TRUE;

            // modules is optional list
            List<String> modules = new ArrayList<>();
            Object modObj = body.get("modules");
            if (modObj instanceof List) {
                for (Object o : (List<?>) modObj)
                    if (o != null)
                        modules.add(String.valueOf(o));
            }

            Admin created = adminService.createAdmin(username, password, name, role, modules, mustChange);
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ok");
            resp.put("admin", Map.of(
                    "username", created.getUsername(),
                    "name", created.getName(),
                    "role", created.getRole(),
                    "accessModules", created.getAccessModules()));
            return ResponseEntity.ok(resp);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to create admin"));
        }
    }

    /**
     * List all admins (Super-Admin or admin manager).
     */
    @GetMapping("/list-admins")
    public ResponseEntity<?> listAdmins() {
        try {
            List<Admin> all = adminService.listAdmins();

            // Build a typed list of maps (no generics ambiguity)
            List<Map<String, Object>> out = all.stream()
                    .map(a -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("username", a.getUsername());
                        m.put("name", a.getName());
                        m.put("role", a.getRole());
                        m.put("active", a.getActive());
                        m.put("accessModules", a.getAccessModules());
                        m.put("mustChangePassword", a.getMustChangePassword());
                        return m;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("status", "ok", "admins", out));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to list admins"));
        }
    }

    /**
     * Deactivate an admin (Super-Admin only).
     */
    @PostMapping("/deactivate-admin/{username}")
    public ResponseEntity<?> deactivateAdmin(@PathVariable String username) {
        try {
            if (!adminService.existsByUsername(username)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "Admin not found"));
            }
            adminService.deactivateAdmin(username);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Admin deactivated"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to deactivate"));
        }
    }

    /**
     * Activate an admin (Super-Admin only).
     */
    @PostMapping("/activate-admin/{username}")
    public ResponseEntity<?> activateAdmin(@PathVariable String username) {
        try {
            if (!adminService.existsByUsername(username)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "Admin not found"));
            }
            adminService.activateAdmin(username);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Admin activated"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to activate"));
        }
    }

    /**
     * Reset password for an admin (Super-Admin only). Returns the temp password in
     * the response.
     * In production you should email the temp password instead of returning it in
     * API response.
     */
    @PostMapping("/reset-password/{username}")
    public ResponseEntity<?> resetPassword(@PathVariable String username,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            if (!adminService.existsByUsername(username)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "Admin not found"));
            }
            String provided = (body != null) ? body.get("tempPassword") : null;
            String temp = adminService.resetPassword(username, provided);
            // Optionally send email with temp password using EmailService here
            return ResponseEntity.ok(Map.of("status", "ok", "tempPassword", temp));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Reset failed"));
        }
    }

    /**
     * Change own password (admin). Expects { "username": "...", "newPassword":"..."
     * }.
     * After successful change, mustChangePassword flag will be cleared.
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String newPass = body.get("newPassword");
        if (username == null || newPass == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "username and newPassword required"));
        }
        try {
            boolean ok = adminService.changePassword(username, newPass);
            if (!ok)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "Admin not found"));
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Password changed"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to change password"));
        }
    }

    /**
     * Update role and modules for an admin (Super-Admin).
     * Body: { "username":"", "role":"PLACEMENT", "modules":["CAREER"] }
     */
    @PostMapping("/update-role-modules")
    public ResponseEntity<?> updateRoleAndModules(@RequestBody Map<String, Object> body) {
        try {
            String username = (String) body.get("username");
            String role = (String) body.get("role");
            List<String> modules = new ArrayList<>();
            Object m = body.get("modules");
            if (m instanceof List) {
                for (Object o : (List<?>) m)
                    if (o != null)
                        modules.add(String.valueOf(o));
            }
            if (username == null || username.isBlank())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", "username required"));
            adminService.updateRoleAndModules(username, role, modules);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Updated"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Update failed"));
        }
    }

    /* ===================== APPROVE BORROW ===================== */

    @PostMapping("/approve/{id}")
    public ResponseEntity<Map<String, Object>> approveBorrow(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();

        String bookCode = (body == null) ? null : body.get("bookCode");
        if (bookCode == null || bookCode.trim().isEmpty()) {
            res.put("status", "error");
            res.put("message", "Book code is required.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
        }

        Optional<BorrowStatus> opt = borrowRepo.findById(id);
        if (opt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Borrow request not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
        }
        BorrowStatus b = opt.get();

        String st = String.valueOf(b.getStatus());
        boolean canApprove = "REQUESTED".equalsIgnoreCase(st) || "PENDING".equalsIgnoreCase(st);
        if (!canApprove || (b.getBookCode() != null && !b.getBookCode().isBlank())) {
            res.put("status", "error");
            res.put("message", "Already accepted or invalid status.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(res);
        }

        Optional<Book> bookOpt = bookRepo.findById(b.getBookId());
        if (bookOpt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Book not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
        }
        Book book = bookOpt.get();

        if (book.getAvailable() == null || book.getAvailable() <= 0) {
            res.put("status", "error");
            res.put("message", "No available copies left.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(res);
        }

        b.setFineAmount(0);
        b.setFinePaid(0);
        b.setStatus("APPROVED");
        b.setBookCode(bookCode.trim());
        b.setBorrowDate(LocalDate.now());
        b.setExpectedReturnDate(LocalDate.now().plusDays(15));
        borrowRepo.save(b);

        book.setAvailable(book.getAvailable() - 1);
        bookRepo.save(book);

        try {
            emailService.sendBorrowApprovedEmail(
                    b.getUserEmail(),
                    book.getTitle(),
                    b.getBookCode(),
                    b.getBorrowDate(),
                    b.getExpectedReturnDate(),
                    book.getPdfPath());
        } catch (Exception ignored) {
        }

        res.put("status", "approved");
        res.put("message", "Request approved and email sent.");
        return ResponseEntity.ok(res);
    }

    /* ===================== APPROVE RETURN ===================== */

    @PostMapping("/approve-return/{id}")
    public ResponseEntity<Map<String, Object>> approveReturn(
            @PathVariable int id,
            @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();

        Optional<BorrowStatus> opt = borrowRepo.findById(id);
        if (opt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Borrow record not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
        }
        BorrowStatus b = opt.get();

        String st = String.valueOf(b.getStatus());
        boolean canApprove = "PENDING_RETURN".equalsIgnoreCase(st) || "RETURN_REQUESTED".equalsIgnoreCase(st);
        if (!canApprove) {
            res.put("status", "error");
            res.put("message", "Return cannot be approved. Current status: " + st);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(res);
        }

        Optional<Book> bookOpt = bookRepo.findById(b.getBookId());
        if (bookOpt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Book not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
        }
        Book book = bookOpt.get();

        LocalDate today = LocalDate.now();
        long daysLate = 0;
        if (b.getExpectedReturnDate() != null && today.isAfter(b.getExpectedReturnDate())) {
            daysLate = ChronoUnit.DAYS.between(b.getExpectedReturnDate(), today);
        }
        int fineAmount = (int) Math.max(0, daysLate);
        b.setFineAmount(fineAmount);

        // Preserve existing finePaid flag from DB (don’t overwrite unless admin says
        // so)
        boolean finePaidFlag = (b.getFinePaid() != null && b.getFinePaid() != 0);
        if (body != null && body.containsKey("finePaid")) {
            Object fp = body.get("finePaid");
            finePaidFlag = (fp instanceof Boolean) ? (Boolean) fp : Boolean.parseBoolean(String.valueOf(fp));
        }

        b.setReturned(true);
        b.setStatus("RETURNED");
        b.setReturnDate(today);
        b.setFinePaid(finePaidFlag ? 1 : 0);
        borrowRepo.save(b);

        // Update book availability
        int avail = (book.getAvailable() == null) ? 0 : book.getAvailable();
        book.setAvailable(avail + 1);
        bookRepo.save(book);

        // derive payment method and reference safely
        String paymentId = Optional.ofNullable(b.getFinePaymentId()).orElse("-");
        String paymentMethod = (finePaidFlag)
                ? (paymentId.startsWith("CASH-") ? "Cash" : "Razorpay")
                : "Unpaid";

        try {
            emailService.sendReturnApprovedEmail(
                    b.getUserEmail(),
                    book.getTitle(),
                    b.getBookCode(),
                    fineAmount,
                    finePaidFlag,
                    paymentMethod,
                    paymentId);
        } catch (Exception ignored) {
        }

        res.put("status", "success");
        res.put("message", "Book return approved and email sent.");
        res.put("fineAmount", fineAmount);
        res.put("finePaid", finePaidFlag);
        res.put("paymentMethod", paymentMethod);
        res.put("paymentId", paymentId);
        return ResponseEntity.ok(res);
    }

    /*
     * ===================== SEND NO-DUE (BLOCK IF ANY ISSUE EXISTS)
     * =====================
     */

    @PostMapping("/send-no-due/{id}")
    public ResponseEntity<Map<String, Object>> sendNoDue(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();

        Optional<BorrowStatus> opt = borrowRepo.findById(id);
        if (opt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Borrow record not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
        }
        BorrowStatus target = opt.get();
        final String userEmail = target.getUserEmail();
        if (userEmail == null || userEmail.isBlank()) {
            res.put("status", "error");
            res.put("message", "User email missing on record.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
        }

        // Load ALL records for this user (works even if exact-ignore-case method isn't
        // present)
        List<BorrowStatus> allForUser;
        try {
            allForUser = borrowRepo.findByUserEmailContainingIgnoreCase(userEmail);
        } catch (Exception e) {
            allForUser = borrowRepo.findAll()
                    .stream()
                    .filter(b -> b.getUserEmail() != null && b.getUserEmail().equalsIgnoreCase(userEmail))
                    .collect(Collectors.toList());
        }

        LocalDate today = LocalDate.now();
        boolean hasBlockingItem = allForUser.stream().anyMatch(b -> {
            String st = String.valueOf(b.getStatus()).toUpperCase(Locale.ROOT);

            boolean notReturned = !b.isReturned();
            boolean pendingState = st.equals("REQUESTED") ||
                    st.equals("PENDING") ||
                    st.equals("APPROVED") ||
                    st.equals("PENDING_RETURN") ||
                    st.equals("RETURN_REQUESTED");

            int fineAmt = Optional.ofNullable(b.getFineAmount()).orElse(0);
            boolean fineUnpaid = fineAmt > 0 && (b.getFinePaid() == null || b.getFinePaid() == 0);

            boolean currentlyOverdue = !b.isReturned()
                    && b.getExpectedReturnDate() != null
                    && today.isAfter(b.getExpectedReturnDate());

            return notReturned || pendingState || fineUnpaid || currentlyOverdue;
        });

        if (hasBlockingItem) {
            res.put("status", "blocked");
            res.put("message", "No-Due cannot be issued: You still have pending returns or unpaid/overdue fines.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
        }

        try {
            emailService.sendGenericEmail(
                    userEmail,
                    "Library No-Due",
                    "Your No-Due is recorded for borrow ID: " + target.getId() + " — " + target.getBookTitle());
        } catch (Exception ignored) {
        }

        res.put("status", "ok");
        res.put("message", "No-Due email sent.");
        return ResponseEntity.ok(res);
    }

    /* ===================== LISTS ===================== */

    @GetMapping("/pending-requests")
    public List<BorrowStatus> getPendingRequests() {
        List<BorrowStatus> out = new ArrayList<>();
        try {
            out.addAll(borrowRepo.findByStatus("REQUESTED"));
        } catch (Exception ignored) {
        }
        try {
            out.addAll(borrowRepo.findByStatus("PENDING"));
        } catch (Exception ignored) {
        }
        out.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        return out;
    }

    @GetMapping("/borrowed-list")
    public List<BorrowStatus> getAllBorrowed() {
        try {
            return borrowRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        } catch (Exception ignored) {
            List<BorrowStatus> list = borrowRepo.findAll();
            list.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
            return list;
        }
    }

    @GetMapping("/borrowed-list/by-email")
    public List<BorrowStatus> getBorrowedByEmail(@RequestParam String email) {
        List<BorrowStatus> list = borrowRepo.findByUserEmailContainingIgnoreCase(email);
        list.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        return list;
    }

    @GetMapping("/registered-users")
    public Set<String> getRegisteredUsers() {
        List<BorrowStatus> all = borrowRepo.findAll();
        Set<String> users = new HashSet<>();
        for (BorrowStatus b : all) {
            if (b.getUserEmail() != null)
                users.add(b.getUserEmail());
        }
        return users;
    }
}
