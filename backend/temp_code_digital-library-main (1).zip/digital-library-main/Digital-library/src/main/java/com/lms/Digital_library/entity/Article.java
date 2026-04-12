package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "articles", indexes = {
        @Index(name = "idx_articles_published", columnList = "publishedAt"),
        @Index(name = "idx_articles_language_score", columnList = "language, score")
})
@Data
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;              // e.g., "The Hindu"
    @Column(unique = true, length = 1000)
    private String guid;                // feed-provided guid or link hash
    private String title;

    @Column(length = 2000)
    private String summary;

    @Column(length = 2000)
    private String link;

    private Instant publishedAt;

    private String language;            // "en" or "te"

    private Long views = 0L;

    private Double score = 0.0;

    private Instant fetchedAt;
}
