package com.lms.Digital_library.service;

import com.lms.Digital_library.config.NewsConfig;
// <-- IMPORTANT: update this import if your Article class uses a different package.
// If Article.java is under com.lms.Digital_library.entity then use:
// import com.lms.Digital_library.entity.Article;
import com.lms.Digital_library.entity.Article;

import com.lms.Digital_library.repository.ArticleRepository;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssFetcherService {

    private final ArticleRepository repo;
    private final NewsConfig newsConfig;

    /**
     * Scheduled fetcher. Interval controlled by application.yml -> news.fetch.intervalMs
     */
    @Scheduled(fixedDelayString = "${news.fetch.intervalMs:300000}")
    @Transactional
    public void fetchAll() {
        if (newsConfig.getFeeds() == null || newsConfig.getFeeds().isEmpty()) {
            log.debug("RssFetcherService: no feeds configured.");
            return;
        }

        for (NewsConfig.FeedBinding f : newsConfig.getFeeds()) {
            try {
                log.debug("Fetching feed '{}' -> {}", f.getName(), f.getUrl());

                // Use URI.create(...) -> then open stream from URL to avoid deprecated URL(String)
                URI uri = URI.create(f.getUrl());
                URL feedUrl = uri.toURL();

                try (InputStream in = feedUrl.openStream()) {
                    SyndFeed feed = new SyndFeedInput().build(new XmlReader(in));
                    for (SyndEntry entry : feed.getEntries()) {
                        String guid = Optional.ofNullable(entry.getUri()).orElse(entry.getLink());
                        if (guid == null || guid.isBlank()) {
                            guid = (entry.getLink() != null ? entry.getLink() : entry.getTitle());
                        }

                        // dedupe
                        if (repo.findByGuid(guid).isPresent()) continue;

                        Article a = new Article();
                        a.setGuid(guid);
                        a.setSource(f.getName());
                        a.setLanguage(f.getLanguage());
                        a.setTitle(entry.getTitle());
                        a.setLink(entry.getLink());
                        a.setSummary(entry.getDescription() != null ? entry.getDescription().getValue() : null);

                        Date pub = entry.getPublishedDate();
                        a.setPublishedAt(pub != null ? pub.toInstant() : Instant.now());
                        a.setFetchedAt(Instant.now());

                        try {
                            repo.save(a);
                        } catch (Exception ex) {
                            log.warn("Failed saving article (guid={}): {}", guid, ex.getMessage());
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch feed {}: {}", f.getUrl(), ex.getMessage());
            }
        }
    }
}
