package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.Borrow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BorrowRepository extends JpaRepository<Borrow, Integer> {
    List<Borrow> findByUserEmail(String userEmail);
}
