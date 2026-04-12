package com.lms.Digital_library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    // default to "*" so missing property doesn’t crash the app/tests
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true);

        if (allowedOrigins == null || allowedOrigins.isBlank() || "*".equals(allowedOrigins.trim())) {
            // use pattern for wildcards (e.g., *.ngrok-free.app)
            cfg.addAllowedOriginPattern("*");
        } else {
            cfg.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .collect(Collectors.toList())
            );
        }

        cfg.addAllowedHeader("*");
        cfg.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(src);
        }
}
