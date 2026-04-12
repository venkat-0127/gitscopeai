package com.lms.Digital_library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "admin")
public class Admin {

    // keep existing primary key to avoid immediate DB break
    @Id
    private String username;

    /**
     * Keep the column name "password" so existing data remains valid.
     * This should store the bcrypt-hashed password (not plaintext).
     */
    private String password;

    /**
     * Optional display name for the admin user.
     */
    private String name;

    /**
     * Role string - e.g. SUPER_ADMIN, LIBRARIAN, PLACEMENT, ARCHIVES
     */
    private String role;

    /**
     * Whether this admin is active. Default true.
     */
    private Boolean active = Boolean.TRUE;

    /**
     * Simple storage of modules/access in JSON or csv form.
     * Column definition set to text so you can store a JSON array string like '["LMS","ARCHIVES"]'
     */
    @Column(name = "access_modules", columnDefinition = "text")
    private String accessModules = "[]";

    /**
     * If true, admin must change password on first login (or next login).
     */
    @Column(name = "must_change_password")
    private Boolean mustChangePassword = Boolean.TRUE;

    /**
     * Timestamp when record created. Will be set on persist if null.
     */
    @Column(name = "created_at", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    // ---------- constructors ----------
    public Admin() {}

    // convenient constructor
    public Admin(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.active = Boolean.TRUE;
        this.accessModules = "[]";
        this.mustChangePassword = Boolean.TRUE;
        this.createdAt = OffsetDateTime.now();
    }

    // ---------- lifecycle ----------
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = OffsetDateTime.now();
        if (this.active == null) this.active = Boolean.TRUE;
        if (this.mustChangePassword == null) this.mustChangePassword = Boolean.TRUE;
        if (this.accessModules == null) this.accessModules = "[]";
    }

    // ---------- getters & setters ----------
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    /**
     * Password field should contain a bcrypt hash. Do NOT store plaintext.
     */
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    /**
     * Raw JSON string describing module access. You can parse it with Jackson when needed.
     * Example: '["LMS","ARCHIVES"]'
     */
    public String getAccessModules() { return accessModules; }
    public void setAccessModules(String accessModules) { this.accessModules = accessModules; }

    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
