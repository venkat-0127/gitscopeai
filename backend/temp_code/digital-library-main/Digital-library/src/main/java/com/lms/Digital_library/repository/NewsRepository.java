package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<NewsArticle, Long>, JpaSpecificationExecutor<NewsArticle> {

    List<NewsArticle> findTop10ByCreatedAtAfterOrderByViewsDesc(LocalDate after);

    List<NewsArticle> findTop10ByOrderByViewsDesc();
}
