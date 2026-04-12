// src/main/java/com/lms/Digital_library/repository/LibrarianRepository.java
package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.Librarian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibrarianRepository extends JpaRepository<Librarian, Long> {
    Optional<Librarian> findByUsername(String username);
}
