package com.lms.Digital_library.repository;
import com.lms.Digital_library.entity.ProjectImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectImportRepository extends JpaRepository<ProjectImport, Long> {
    Page<ProjectImport> findByTitleContainingIgnoreCaseOrOwnerNameContainingIgnoreCase(String t1, String t2, Pageable p);
}
