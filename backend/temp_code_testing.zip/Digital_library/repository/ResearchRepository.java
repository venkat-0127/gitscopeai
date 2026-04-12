package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.ResearchPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResearchRepository extends JpaRepository<ResearchPaper, Long> {
    Page<ResearchPaper> findByTitleContainingIgnoreCaseOrAuthorsContainingIgnoreCaseOrTagsContainingIgnoreCase(
            String title, String authors, String tags, Pageable pageable);

    // new method
    Page<ResearchPaper> findByUploadedBy(String uploadedBy, Pageable pageable);
}
