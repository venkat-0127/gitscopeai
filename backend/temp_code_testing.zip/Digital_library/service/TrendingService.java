package com.lms.Digital_library.service;

import com.lms.Digital_library.config.NewsConfig;
// <-- IMPORTANT: update this import if your Article class uses a different package.
// If Article.java is under com.lms.Digital_library.entity then use:
// import com.lms.Digital_library.entity.Article;
import com.lms.Digital_library.entity.Article;

import com.lms.Digital_library.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingService {

    private final ArticleRepository repo;
    private final NewsConfig newsConfig;

    @Scheduled(fixedDelayString = "${news.trending.recalcMs:300000}")
    @Transactional
    public void recalcTrendingScores() {
        long ttlHours = Optional.ofNullable(newsConfig.getTrending()).map(NewsConfig.Trending::getTtlHours).orElse(168L);
        double recencyWeight = Optional.ofNullable(newsConfig.getTrending()).map(NewsConfig.Trending::getRecencyWeight).orElse(2.0);
        double popularityWeight = Optional.ofNullable(newsConfig.getTrending()).map(NewsConfig.Trending::getPopularityWeight).orElse(1.0);

        Instant now = Instant.now();
        Instant since = now.minus(ttlHours, ChronoUnit.HOURS);
        List<Article> recent = repo.findAllByPublishedAtAfter(since);

        if (recent.isEmpty()) {
            log.debug("TrendingService: no recent articles to score.");
            return;
        }

        Map<String, Long> titleCounts = recent.stream()
                .collect(Collectors.groupingBy(a -> normalizeKey(a.getTitle()), Collectors.counting()));

        for (Article a : recent) {
            double recencyFactor = 0.0;
            if (a.getPublishedAt() != null) {
                double ageSec = Duration.between(a.getPublishedAt(), now).toSeconds();
                double ttlSec = ttlHours * 3600.0;
                recencyFactor = Math.max(0.0, 1.0 - (ageSec / ttlSec));
            }

            double popFactor = Math.log(1 + Optional.ofNullable(a.getViews()).orElse(0L));
            double dupBoost = Math.min(1.0, titleCounts.getOrDefault(normalizeKey(a.getTitle()), 1L) * 0.25);
            double score = recencyWeight * recencyFactor + popularityWeight * popFactor + dupBoost;
            a.setScore(score);
        }

        repo.saveAll(recent);
        log.debug("TrendingService: recalculated scores for {} articles", recent.size());
    }

    private String normalizeKey(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
