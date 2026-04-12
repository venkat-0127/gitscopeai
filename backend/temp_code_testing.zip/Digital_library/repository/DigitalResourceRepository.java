package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.DigitalResource;
import com.lms.Digital_library.entity.DigitalResource.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DigitalResourceRepository extends JpaRepository<DigitalResource, Long> {

    @Query("""
        SELECT r FROM DigitalResource r
        WHERE (:cat IS NULL OR r.category = :cat)
          AND (
              :q IS NULL OR :q = '' OR
              LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(r.description) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(COALESCE(r.tags,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(COALESCE(r.provider,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR
              LOWER(COALESCE(r.authors,'')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
    """)
    Page<DigitalResource> search(@Param("q") String q, @Param("cat") Category cat, Pageable pageable);
}
