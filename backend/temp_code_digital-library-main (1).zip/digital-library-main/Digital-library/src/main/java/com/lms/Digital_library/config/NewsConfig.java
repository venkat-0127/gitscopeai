package com.lms.Digital_library.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "news")
@Data
public class NewsConfig {

    private Fetch fetch = new Fetch();
    private Trending trending = new Trending();
    private List<FeedBinding> feeds;

    @Data
    public static class Fetch {
        private long intervalMs = 300000; // default 5 minutes
    }

    @Data
    public static class Trending {
        private long ttlHours = 168; // 7 days by default
        private double recencyWeight = 2.0;
        private double popularityWeight = 1.0;
        private long recalcMs = 300000; // 5 minutes
    }

    @Data
    public static class FeedBinding {
        private String name;
        private String url;
        private String language;
    }
}
