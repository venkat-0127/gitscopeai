package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByGuid(String guid);

    List<Article> findTop100BySourceOrderByPublishedAtDesc(String source);

    Page<Article> findByTitleContainingIgnoreCaseOrSummaryContainingIgnoreCase(String title, String summary, Pageable pageable);

    List<Article> findTop50ByLanguageOrderByScoreDesc(String language);

    List<Article> findAllByPublishedAtAfter(Instant since);

    @Modifying
    @Transactional
    @Query("update Article a set a.views = a.views + 1 where a.id = :id")
    void incrementViews(@Param("id") Long id);
}
