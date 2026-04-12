package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.Book;
import com.lms.Digital_library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin
public class BookController {

    private final Path pdfStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    @Autowired
    private BookRepository bookRepo;

    public BookController() throws IOException {
        Files.createDirectories(pdfStorageLocation);
    }

    // ✅ Add Book with PDF + Category + Rack
    @PostMapping("/add")
    public ResponseEntity<String> addBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("semester") int semester,
            @RequestParam("year") int year,
            @RequestParam("available") int available,
            @RequestParam("rack") String rack,
            @RequestParam("category") String category,
            @RequestParam("pdf") MultipartFile file
    ) {
        try {
            // Save PDF file
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path targetLocation = pdfStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Save Book entity
            Book book = new Book();
            book.setTitle(title);
            book.setAuthor(author);
            book.setSemester(semester);
            book.setYear(year);
            book.setAvailable(available);
            book.setPdfPath(fileName);
            book.setRack(rack);
            book.setCategory(category);

            bookRepo.save(book);

            return ResponseEntity.ok("✅ Book added successfully.");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("❌ Failed to upload book.");
        }
    }

    // 📖 Get All Books
    @GetMapping("/all")
    public List<Book> getAllBooks() {
        return bookRepo.findAll();
    }

    // 📂 View PDF File
    @GetMapping("/pdf/{id}")
    public ResponseEntity<Resource> viewPdf(@PathVariable int id) throws MalformedURLException {
        Book book = bookRepo.findById(id).orElse(null);
        if (book == null || book.getPdfPath() == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = pdfStorageLocation.resolve(book.getPdfPath()).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // 🗑️ Delete Book
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable int id) {
        if (!bookRepo.existsById(id)) {
            return ResponseEntity.status(404).body("❌ Book not found.");
        }

        bookRepo.deleteById(id);
        return ResponseEntity.ok("🗑️ Book deleted successfully.");
    }

    // 🔎 Get Book by ID
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable int id) {
        Optional<Book> book = bookRepo.findById(id);
        return book.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✏️ Update/Edit Book (with optional PDF replacement + category)
    @PutMapping("/{id}")
    public ResponseEntity<String> editBook(
            @PathVariable("id") int id,
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("semester") int semester,
            @RequestParam("year") int year,
            @RequestParam("available") int available,
            @RequestParam("rack") String rack,
            @RequestParam("category") String category,
            @RequestParam(value = "pdf", required = false) MultipartFile file
    ) {
        Optional<Book> optionalBook = bookRepo.findById(id);
        if (optionalBook.isPresent()) {
            Book book = optionalBook.get();

            book.setTitle(title);
            book.setAuthor(author);
            book.setSemester(semester);
            book.setYear(year);
            book.setAvailable(available);
            book.setRack(rack);
            book.setCategory(category);

            if (file != null && !file.isEmpty()) {
                try {
                    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    Path targetLocation = pdfStorageLocation.resolve(fileName);
                    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                    book.setPdfPath(fileName);
                } catch (IOException e) {
                    return ResponseEntity.status(500).body("❌ Failed to upload new file.");
                }
            }

            bookRepo.save(book);
            return ResponseEntity.ok("✅ Book updated successfully.");
        } else {
            return ResponseEntity.status(404).body("❌ Book not found.");
        }
    }
}
