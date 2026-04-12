package com.lms.Digital_library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.Digital_library.entity.Admin;
import com.lms.Digital_library.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.security.SecureRandom;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * AdminService - handles admin creation, authentication and management.
 *
 * Note: this service assumes passwords stored in Admin.password are bcrypt hashes.
 *       Use PasswordEncoder bean (BCryptPasswordEncoder) in your configuration.
 */
@Service
public class AdminService {

    private final AdminRepository adminRepo;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Optional server-side pepper (stored in environment). If set, it is prepended to raw password
     * before hashing and verifying.
     */
    @Value("${SECURITY_PEPPER:}")
    private String pepper;

    public AdminService(AdminRepository adminRepo, PasswordEncoder passwordEncoder) {
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /* ----------------- Authentication ------------------ */

    /**
     * Authenticate admin by username + raw password.
     *
     * @return the Admin object if login successful; empty if not.
     */
    public Optional<Admin> login(String username, String rawPassword) {
        if (username == null || rawPassword == null) return Optional.empty();
        Optional<Admin> opt = adminRepo.findByUsername(username);
        if (opt.isEmpty()) return Optional.empty();
        Admin admin = opt.get();
        if (Boolean.FALSE.equals(admin.getActive())) return Optional.empty();
        final String candidate = (pepper == null ? "" : pepper) + rawPassword;
        if (!passwordEncoder.matches(candidate, admin.getPassword())) return Optional.empty();
        // Successful login
        return Optional.of(admin);
    }

    /* ----------------- CRUD / Management ------------------ */

    /**
     * Create a new admin. Password will be hashed.
     *
     * @param username    unique username (email recommended)
     * @param rawPassword initial raw password (will be hashed)
     * @param name        display name (optional)
     * @param role        role string, e.g. SUPER_ADMIN, LIBRARIAN, PLACEMENT, ARCHIVES
     * @param modules     list of modules allowed, e.g. ["LMS","ARCHIVES"]
     * @param mustChangePassword whether the admin must change password on first login
     * @return created Admin
     * @throws IllegalArgumentException if username already exists
     */
    public Admin createAdmin(String username,
                             String rawPassword,
                             String name,
                             String role,
                             List<String> modules,
                             boolean mustChangePassword) {

        if (username == null || username.isBlank()) throw new IllegalArgumentException("username required");
        if (rawPassword == null || rawPassword.isBlank())
            throw new IllegalArgumentException("password required");

        if (adminRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("username already exists: " + username);
        }

        String toHash = (pepper == null ? "" : pepper) + rawPassword;
        String hashed = passwordEncoder.encode(toHash);

        Admin a = new Admin();
        a.setUsername(username);
        a.setPassword(hashed);
        a.setName(name);
        a.setRole(role == null ? "ADMIN" : role);
        a.setActive(Boolean.TRUE);
        a.setMustChangePassword(mustChangePassword);
        a.setCreatedAt(OffsetDateTime.now());
        try {
            String modulesJson = (modules == null ? "[]" : objectMapper.writeValueAsString(modules));
            a.setAccessModules(modulesJson);
        } catch (JsonProcessingException e) {
            // fallback to empty array
            a.setAccessModules("[]");
        }
        return adminRepo.save(a);
    }

    /**
     * List all admins (optionally later add pagination/filter).
     */
    public List<Admin> listAdmins() {
        return adminRepo.findAll();
    }

    /**
     * Find admin by username.
     */
    public Optional<Admin> getAdmin(String username) {
        return adminRepo.findByUsername(username);
    }

    /**
     * Simple existence check.
     */
    public boolean existsByUsername(String username) {
        return adminRepo.existsByUsername(username);
    }

    /**
     * Deactivate an admin (soft disable).
     */
    public void deactivateAdmin(String username) {
        adminRepo.findByUsername(username).ifPresent(admin -> {
            admin.setActive(Boolean.FALSE);
            adminRepo.save(admin);
        });
    }

    /**
     * Activate an admin.
     */
    public void activateAdmin(String username) {
        adminRepo.findByUsername(username).ifPresent(admin -> {
            admin.setActive(Boolean.TRUE);
            adminRepo.save(admin);
        });
    }

    /**
     * Update role and access modules for an admin.
     *
     * @param username admin username
     * @param newRole  new role (non-null)
     * @param modules  list of modules (e.g. ["LMS","ARCHIVES"])
     */
    public void updateRoleAndModules(String username, String newRole, List<String> modules) {
        adminRepo.findByUsername(username).ifPresent(admin -> {
            if (newRole != null && !newRole.isBlank()) admin.setRole(newRole);
            try {
                String modulesJson = (modules == null ? "[]" : objectMapper.writeValueAsString(modules));
                admin.setAccessModules(modulesJson);
            } catch (JsonProcessingException e) {
                // ignore and keep existing modules
            }
            adminRepo.save(admin);
        });
    }

    /**
     * Change own password (admin changes their password). Clears mustChangePassword flag.
     *
     * @param username    username
     * @param rawPassword new raw password
     * @return true if changed, false if user not found
     */
    public boolean changePassword(String username, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) throw new IllegalArgumentException("password required");
        Optional<Admin> opt = adminRepo.findByUsername(username);
        if (opt.isEmpty()) return false;
        Admin admin = opt.get();
        String toHash = (pepper == null ? "" : pepper) + rawPassword;
        admin.setPassword(passwordEncoder.encode(toHash));
        admin.setMustChangePassword(Boolean.FALSE);
        adminRepo.save(admin);
        return true;
    }

    /**
     * Reset password (Super-Admin action). Sets a provided temp password (or returns generated).
     * Caller should send this temp password to the user via secure channel and mark mustChangePassword = true.
     *
     * @param username  admin username to reset
     * @param tempPass  if null, method will generate a random temp pass and return it
     * @return the temp password that was set (either provided or generated)
     * @throws NoSuchElementException if admin not found
     */
    public String resetPassword(String username, String tempPass) {
        Admin admin = adminRepo.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("admin not found: " + username));
        final String newTemp = (tempPass == null || tempPass.isBlank()) ? generateTempPassword() : tempPass;
        String toHash = (pepper == null ? "" : pepper) + newTemp;
        admin.setPassword(passwordEncoder.encode(toHash));
        admin.setMustChangePassword(Boolean.TRUE);
        adminRepo.save(admin);
        return newTemp;
    }

    private String generateTempPassword() {
    // simple random 12-char password (you can replace with stronger generator)
    final String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#%!";
    SecureRandom rnd = new SecureRandom();
    StringBuilder sb = new StringBuilder(12);
    for (int i = 0; i < 12; i++) {
        sb.append(chars.charAt(rnd.nextInt(chars.length())));
    }
    return sb.toString();
}


    /* ----------------- Helpers ------------------ */

    /**
 * Convenience: parse accessModules JSON to list.
 */
    public List<String> getModulesForAdmin(Admin admin) {
    if (admin == null) return Collections.emptyList();
    String json = admin.getAccessModules();
    if (json == null || json.isBlank()) return Collections.emptyList();
    try {
        return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (Exception e) {
        return Collections.emptyList();
    }
    }
}