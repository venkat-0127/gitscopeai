// src/main/java/com/lms/Digital_library/controller/LibrarianAdminController.java
package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.Librarian;
import com.lms.Digital_library.repository.LibrarianRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/librarians")
@CrossOrigin(origins = "*")
public class LibrarianAdminController {

    private final LibrarianRepository librarianRepository;
    private final PasswordEncoder passwordEncoder;

    public LibrarianAdminController(LibrarianRepository librarianRepository,
                                    PasswordEncoder passwordEncoder) {
        this.librarianRepository = librarianRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1) List all librarians (for SUPER ADMIN dashboard)
    @GetMapping
    public List<Librarian> all() {
        return librarianRepository.findAll();
    }

    // 2) Create new librarian: SUPER ADMIN sends username + password
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String rawPassword = body.getOrDefault("password", "");

        if (username.isEmpty() || rawPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", "Username and password required")
            );
        }

        if (librarianRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(409).body(
                    Map.of("status", "error", "message", "Username already exists")
            );
        }

        Librarian lib = new Librarian();
        lib.setUsername(username);
        lib.setPasswordHash(passwordEncoder.encode(rawPassword));
        lib.setActive(true);           // default active
        // name/email/phone/staffId stay null

        librarianRepository.save(lib);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Librarian created"));
    }

    // 3) Activate / deactivate
    @PatchMapping("/{id}/active")
    public ResponseEntity<?> setActive(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body) {

        boolean active = Boolean.parseBoolean(
                String.valueOf(body.getOrDefault("active", "false"))
        );

        return librarianRepository.findById(id)
                .map(lib -> {
                    lib.setActive(active);
                    librarianRepository.save(lib);
                    return ResponseEntity.ok(Map.of("status", "success"));
                })
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("status", "error", "message", "Librarian not found")));
    }

    // 4) Delete librarian
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!librarianRepository.existsById(id)) {
            return ResponseEntity.status(404)
                    .body(Map.of("status", "error", "message", "Librarian not found"));
        }
        librarianRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
