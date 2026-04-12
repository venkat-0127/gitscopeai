package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorrowStatusRepository extends JpaRepository<BorrowStatus, Integer> {

    List<BorrowStatus> findByUserEmailAndReturnedFalse(String email);

    List<BorrowStatus> findByReturnedFalse();

    List<BorrowStatus> findByReturnedTrue();

    List<BorrowStatus> findByUserEmailAndStatusAndReturnedFalse(String email, String status);

    List<BorrowStatus> findByStatus(String status);

    List<BorrowStatus> findByUserEmailAndStatus(String email, String status);

    // ✅ NEW: For showing latest borrowed books (student/teacher dashboard)
    List<BorrowStatus> findByUserEmailOrderByIdDesc(String email);

    // ✅ NEW: For admin filter by email (fixes your error)
    List<BorrowStatus> findByUserEmailContainingIgnoreCase(String email);
}
