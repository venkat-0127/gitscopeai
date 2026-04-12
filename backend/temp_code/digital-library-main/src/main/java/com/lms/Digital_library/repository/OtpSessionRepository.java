package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.OtpSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpSessionRepository extends JpaRepository<OtpSession, Long> {
    Optional<OtpSession> findTopByEmailOrderByCreatedAtDesc(String email);
}
