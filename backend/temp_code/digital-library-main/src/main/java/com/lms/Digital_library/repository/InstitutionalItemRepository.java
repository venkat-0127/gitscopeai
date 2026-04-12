package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.InstitutionalItem;
import com.lms.Digital_library.entity.InstitutionalItem.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InstitutionalItemRepository extends JpaRepository<InstitutionalItem, Long> {

    @Query(
        value = """
            SELECT i FROM InstitutionalItem i
            WHERE
                ( :q IS NULL OR :q = '' OR
                  LOWER(i.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(COALESCE(i.summary, ''))  LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(COALESCE(i.tags, ''))     LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(COALESCE(i.authors, ''))  LIKE LOWER(CONCAT('%', :q, '%'))
                )
            AND ( :type IS NULL OR i.type = :type )
            AND ( :dept IS NULL OR :dept = '' OR LOWER(COALESCE(i.department, '')) = LOWER(:dept) )
            AND ( :year IS NULL OR i.year = :year )
            ORDER BY i.createdAt DESC
            """,
        countQuery = """
            SELECT COUNT(i) FROM InstitutionalItem i
            WHERE
                ( :q IS NULL OR :q = '' OR
                  LOWER(i.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(COALESCE(i.summary, ''))  LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(COALESCE(i.tags, ''))     LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(COALESCE(i.authors, ''))  LIKE LOWER(CONCAT('%', :q, '%'))
                )
            AND ( :type IS NULL OR i.type = :type )
            AND ( :dept IS NULL OR :dept = '' OR LOWER(COALESCE(i.department, '')) = LOWER(:dept) )
            AND ( :year IS NULL OR i.year = :year )
            """
    )
    Page<InstitutionalItem> search(
            @Param("q") String q,
            @Param("type") Type type,
            @Param("dept") String dept,
            @Param("year") Integer year,
            Pageable pageable
    );
}
