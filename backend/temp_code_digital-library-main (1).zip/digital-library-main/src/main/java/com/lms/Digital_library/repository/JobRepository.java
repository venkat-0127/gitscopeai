package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findBySourceAndSourceId(String source, String sourceId);

    Page<Job> findByJobTypeIgnoreCase(String jobType, Pageable pageable);

    Page<Job> findByTitleContainingIgnoreCaseOrCompanyContainingIgnoreCaseOrLocationContainingIgnoreCase(
            String title, String company, String location, Pageable pageable);
}
