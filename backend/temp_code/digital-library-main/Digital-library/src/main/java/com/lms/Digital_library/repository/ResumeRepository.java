package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.ResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResumeRepository extends JpaRepository<ResumeEntity, Long> {
    List<ResumeEntity> findByStudentId(String studentId);
}
