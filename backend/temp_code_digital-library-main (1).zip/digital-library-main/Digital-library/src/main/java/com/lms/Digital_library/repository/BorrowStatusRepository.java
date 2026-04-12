package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BorrowStatusRepository extends JpaRepository<BorrowStatus, Integer> {

    List<BorrowStatus> findByUserEmailAndReturnedFalse(String email);

    List<BorrowStatus> findByReturnedFalse();

    List<BorrowStatus> findByReturnedTrue();

    List<BorrowStatus> findByUserEmailAndStatusAndReturnedFalse(String email, String status);

    List<BorrowStatus> findByStatus(String status);

    
    List<BorrowStatus> findByUserEmailIgnoreCase(String userEmail);

    List<BorrowStatus> findByUserEmailAndStatus(String email, String status);

    // Student/Teacher dashboard – latest first
    List<BorrowStatus> findByUserEmailOrderByIdDesc(String email);

    // Admin quick filter by email
    List<BorrowStatus> findByUserEmailContainingIgnoreCase(String email);

    // Helpful for no-due checks / dashboards
    boolean existsByUserEmailAndStatusIn(String email, Collection<String> statuses);

    // Optional (sometimes handy)
    List<BorrowStatus> findByUserEmailAndBookCategoryContainingIgnoreCaseOrderByIdDesc(String email, String bookCategory);

}
