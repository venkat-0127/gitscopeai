package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {

    /**
     * Find an admin by username (primary key).
     */
    Optional<Admin> findByUsername(String username);

    /**
     * Check if a username already exists.
     */
    boolean existsByUsername(String username);

    /**
     * Find all active admins (used for listing in the Manage Admins panel).
     */
    List<Admin> findByActiveTrue();

    /**
     * Find all admins by role.
     */
    List<Admin> findByRole(String role);

    /**
     * Optional custom query example to fetch limited info if you want.
     */
    @Query("SELECT a FROM Admin a WHERE a.active = true ORDER BY a.createdAt DESC")
    List<Admin> findAllActiveOrdered();

    /**
     * 🔒 Legacy fallback method:
     * Avoid using this for authentication since passwords are now hashed.
     * Use service-layer verification with PasswordEncoder instead.
     */
    @Deprecated
    default Optional<Admin> findByUsernameAndPassword(String username, String rawPassword) {
        throw new UnsupportedOperationException(
            "Use AdminService.login() with PasswordEncoder for secure password matching."
        );
    }
}
