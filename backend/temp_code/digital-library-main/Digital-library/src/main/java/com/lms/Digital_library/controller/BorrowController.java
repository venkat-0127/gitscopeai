package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.Book;
import com.lms.Digital_library.entity.BorrowStatus;
import com.lms.Digital_library.repository.BookRepository;
import com.lms.Digital_library.repository.BorrowStatusRepository;
import com.lms.Digital_library.service.EmailService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/borrow")
@CrossOrigin
public class BorrowController {

    @Autowired private BorrowStatusRepository borrowRepo;
    @Autowired private BookRepository bookRepo;
    @Autowired private EmailService emailService;

    // util: try method names until one exists
    private static Object tryCall(Object o, String... methods) {
        for (String m : methods) {
            try {
                Method md = o.getClass().getMethod(m);
                return md.invoke(o);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /* CREATE BORROW REQUEST */
    @PostMapping("/request")
    public Map<String, Object> requestBorrow(@RequestParam String email,
                                             @RequestParam Integer bookId) {
        Map<String, Object> res = new HashMap<>();

        Optional<Book> bookOpt = bookRepo.findById(bookId);
        if (bookOpt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Book not found.");
            return res;
        }
        Book book = bookOpt.get();

        // available (supports getAvailable() or getStock() or none)
        Integer available = null;
        Object availObj = tryCall(book, "getAvailable", "getStock");
        if (availObj != null) {
            available = (availObj instanceof Integer) ? (Integer) availObj : Integer.valueOf(availObj.toString());
        }
        if (available != null && available <= 0) {
            res.put("status", "error");
            res.put("message", "No copies available.");
            return res;
        }

        BorrowStatus b = new BorrowStatus();
        b.setUserEmail(email);
        b.setBookId(bookId);

        // title
        Object title = tryCall(book, "getTitle", "getBookTitle", "getName");
        b.setBookTitle(title == null ? "Unknown" : title.toString());

        // code (optional)
        Object code = tryCall(book, "getCode", "getBookCode");
        if (code != null) b.setBookCode(code.toString());

        // category (optional)
        Object cat = tryCall(book, "getCategory", "getBookCategory", "getProgram");
        if (cat != null) b.setBookCategory(cat.toString());

        // defaults
        b.setRequestDate(LocalDate.now());
        b.setBorrowDate(LocalDate.now());
        b.setExpectedReturnDate(LocalDate.now().plusDays(15));
        b.setReturned(false);
        b.setStatus("REQUESTED");

        // fines (DB NOT NULL)
        b.setFine(0);
        b.setFineAmount(0);
        b.setFinePaid(0);
        b.setFineLegacy(0);

        borrowRepo.save(b);

        res.put("status", "success");
        res.put("message", "Borrow request sent.");
        res.put("borrowId", b.getId());
        return res;
    }

    /* STUDENT -> REQUEST RETURN */
    @PostMapping("/request-return/{id}")
    public Map<String, Object> requestReturn(@PathVariable Integer id,
                                             @RequestParam(required = false) String note) {
        Map<String, Object> res = new HashMap<>();
        Optional<BorrowStatus> opt = borrowRepo.findById(id);
        if (opt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Borrow record not found.");
            return res;
        }
        BorrowStatus b = opt.get();

        String s = b.getStatus() == null ? "" : b.getStatus();
        if (!(s.equalsIgnoreCase("APPROVED") || s.equalsIgnoreCase("BORROWED") || s.equalsIgnoreCase("FINE_PAID"))) {
            res.put("status", "error");
            res.put("message", "Cannot request return. Current status: " + s);
            return res;
        }

        b.setStatus("RETURN_REQUESTED");
        borrowRepo.save(b);

        try {
            String adminEmail = "venkatthota9381@gmail.com";
            String subject = "Return Requested: " + b.getBookTitle();
            String body = "User " + b.getUserEmail()
                    + " requested to return '" + b.getBookTitle()
                    + "' (Borrow ID: " + b.getId() + ").";
            emailService.sendGenericEmail(adminEmail, subject, body);
        } catch (Exception ignored) {}

        res.put("status", "requested");
        res.put("message", "Return request submitted. Admin will approve it.");
        return res;
    }

    /* DIRECT RETURN (legacy/manual) */
    @PostMapping("/return/{id}")
    public Map<String, Object> returnBook(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        Optional<BorrowStatus> opt = borrowRepo.findById(id);
        if (opt.isEmpty()) {
            res.put("status", "error");
            res.put("message", "Borrow record not found.");
            return res;
        }

        BorrowStatus b = opt.get();

        if (b.isReturned()) {
            res.put("status", "error");
            res.put("message", "Already returned.");
            return res;
        }

        int fineAmount = 0;
        if (b.getExpectedReturnDate() != null
                && b.getUserEmail() != null
                && b.getUserEmail().startsWith("22")) {
            LocalDate now = LocalDate.now();
            long overdue = java.time.temporal.ChronoUnit.DAYS.between(b.getExpectedReturnDate(), now);
            if (overdue > 0) fineAmount = (int) overdue; // ₹1/day
        }

        b.setReturned(true);
        b.setStatus("RETURNED");
        b.setReturnDate(LocalDate.now());
        b.setFineAmount(fineAmount);
        borrowRepo.save(b);

        // increment stock if available field exists
        bookRepo.findById(b.getBookId()).ifPresent(book -> {
            try {
                Object avObj = tryCall(book, "getAvailable", "getStock");
                if (avObj != null) {
                    int av = (avObj instanceof Integer) ? (Integer) avObj : Integer.parseInt(avObj.toString());
                    Method setter = book.getClass().getMethod(avObj instanceof Integer ? "setAvailable" : "setStock", int.class);
                    setter.invoke(book, av + 1);
                    bookRepo.save(book);
                }
            } catch (Exception ignored) {}
        });

        res.put("status", "returned");
        res.put("fineAmount", b.getFineAmount());
        res.put("finePaid", b.getFinePaid());
        return res;
    }

    /* SIMULATED FINE PAYMENT */
    @PostMapping("/pay-fine/{id}")
    public Map<String, Object> payFine(@PathVariable Integer id) {
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

        b.setFinePaid(b.getFineAmount());
        b.setStatus("FINE_PAID");
        borrowRepo.save(b);

        try {
            emailService.sendPaymentReceivedEmail(
                    b.getUserEmail(), b.getBookTitle(), b.getId(), b.getFineAmount()
            );
        } catch (Exception ignored) {}

        res.put("status", "success");
        res.put("message", "Fine paid successfully (simulated).");
        res.put("fineAmount", b.getFineAmount());
        res.put("finePaid", b.getFinePaid());
        return res;
    }

    /* REAL PAYMENT CONFIRM */
    @PostMapping("/mark-fine-paid/{id}")
    public ResponseEntity<?> markFinePaidReal(@PathVariable Integer id,
                                              @RequestBody(required = false) Map<String, Object> body) {
        Optional<BorrowStatus> opt = borrowRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Borrow record not found."));
        }
        BorrowStatus b = opt.get();

        int paid = (b.getFineAmount() == null ? 0 : b.getFineAmount());
        if (body != null && body.get("amountRupees") != null) {
            try { paid = (int)Math.round(Double.parseDouble(body.get("amountRupees").toString())); } catch (Exception ignored) {}
        }
        b.setFinePaid(paid);
        if (body != null && body.get("paymentId") != null) b.setFinePaymentId(String.valueOf(body.get("paymentId")));
        b.setStatus("FINE_PAID");
        borrowRepo.save(b);

        try {
            emailService.sendPaymentReceivedEmail(
                    b.getUserEmail(), b.getBookTitle(), b.getId(), b.getFineAmount()
            );
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Fine marked as paid",
                "fineAmount", b.getFineAmount(),
                "finePaid", b.getFinePaid()
        ));
    }

    /* FINE HISTORY */
    @GetMapping("/fine-history")
    public List<BorrowStatus> fineHistory(@RequestParam String email) {
        List<BorrowStatus> mine = borrowRepo.findByUserEmailOrderByIdDesc(email);
        List<BorrowStatus> out = new ArrayList<>();
        for (BorrowStatus b : mine) {
            if ((b.getFineAmount() != null && b.getFineAmount() > 0) ||
                (b.getFinePaid() != null && b.getFinePaid() > 0)) {
                out.add(b);
            }
        }
        return out;
    }

    /* NO-DUE PDF */
    @GetMapping("/no-due/download")
    public ResponseEntity<?> downloadNoDue(@RequestParam String email) {
        List<BorrowStatus> mine = borrowRepo.findByUserEmailOrderByIdDesc(email);

        boolean hasUnreturned = mine.stream().anyMatch(b -> !b.isReturned());
        boolean hasUnpaidFine = mine.stream().anyMatch(b ->
                (b.getFineAmount() != null ? b.getFineAmount() : 0) -
                (b.getFinePaid()   != null ? b.getFinePaid()   : 0) > 0);

        if (hasUnreturned || hasUnpaidFine) {
            String reason = hasUnreturned ? "You have unreturned books." : "You have unpaid fines.";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "blocked", "message", "No-Due cannot be issued: " + reason));
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - 80;
                float left = 70;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                cs.newLineAtOffset(left, y);
                cs.showText("Library No-Due Certificate");
                cs.endText();

                y -= 30;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(left, y);
                cs.showText("Date: " + today);
                cs.endText();

                y -= 20;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(left, y);
                cs.showText("Issued To: " + email);
                cs.endText();

                y -= 30;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(left, y);
                cs.showText("This is to certify that the above student has no pending");
                cs.endText();

                y -= 16;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(left, y);
                cs.showText("library dues (no books to return and no unpaid fines) as of the date above.");
                cs.endText();

                y -= 50;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(left, y);
                cs.showText("Librarian Signature:");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("NoDue-" + email.replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf")
                    .build());

            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to generate PDF"));
        }
    }

    /* LISTS */
    @GetMapping("/my-books")
    public List<BorrowStatus> getMyBooks(@RequestParam String email) {
        return borrowRepo.findByUserEmailOrderByIdDesc(email);
    }

    @GetMapping("/my-books/program")
    public List<BorrowStatus> getMyBooksByProgram(@RequestParam String email,
                                                  @RequestParam String program) {
        String p = program.toLowerCase();
        List<BorrowStatus> out = new ArrayList<>();
        for (BorrowStatus b : borrowRepo.findByUserEmailOrderByIdDesc(email)) {
            String cat = String.valueOf(b.getBookCategory()).toLowerCase();
            if (cat.contains(p)) out.add(b);
        }
        return out;
    }

    @GetMapping("/all-borrowed")
    public List<BorrowStatus> getAllBorrowedBooks() {
        return borrowRepo.findAll();
    }
}
