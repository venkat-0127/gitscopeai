package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.LoginRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginRecordRepository extends JpaRepository<LoginRecord, Long> {
}
