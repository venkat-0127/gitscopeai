package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.ProjectItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectItemRepository extends JpaRepository<ProjectItem, Long> {

    @Query("""
        SELECT p FROM ProjectItem p
        WHERE (:category IS NULL OR p.category = :category)
          AND (
              :q IS NULL
              OR LOWER(p.title)     LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(p.owner)     LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(p.techStack) LIKE LOWER(CONCAT('%', :q, '%'))
          )
    """)
    Page<ProjectItem> search(
        @Param("category") ProjectItem.ProjectCategory category,
        @Param("q") String q,
        Pageable pageable
    );
}
