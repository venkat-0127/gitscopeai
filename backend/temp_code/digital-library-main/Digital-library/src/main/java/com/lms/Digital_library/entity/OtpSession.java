package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class OtpSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String otp;
    private LocalDateTime createdAt;

    public OtpSession() {}

    public OtpSession(String email, String otp, LocalDateTime createdAt) {
        this.email = email;
        this.otp = otp;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getOtp() { return otp; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setOtp(String otp) { this.otp = otp; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
