// src/main/java/com/lms/Digital_library/controller/LibrarianAuthController.java
package com.lms.Digital_library.controller;

import com.lms.Digital_library.entity.Librarian;
import com.lms.Digital_library.repository.LibrarianRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/librarian")
@CrossOrigin(origins = "*")
public class LibrarianAuthController {

        private final LibrarianRepository repo;
        private final PasswordEncoder passwordEncoder;

        public LibrarianAuthController(LibrarianRepository repo,
                        PasswordEncoder passwordEncoder) {
                this.repo = repo;
                this.passwordEncoder = passwordEncoder;
        }

        @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
                String username = body.getOrDefault("username", "").trim();
                String password = body.getOrDefault("password", "");

                return repo.findByUsername(username)
                                .filter(Librarian::isActive)
                                .filter(lib -> passwordEncoder.matches(password, lib.getPasswordHash()))
                                .<ResponseEntity<?>>map(lib -> ResponseEntity.ok(Map.of(
                                                "status", "success",
                                                "role", "LIBRARIAN",
                                                "username", lib.getUsername())))
                                .orElseGet(() -> ResponseEntity.status(401).body(
                                                Map.of("status", "error",
                                                                "message", "Invalid credentials or account inactive")));
        }
}
