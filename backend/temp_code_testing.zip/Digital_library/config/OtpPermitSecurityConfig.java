package com.lms.Digital_library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class OtpPermitSecurityConfig {

    // Only apply this filter chain to /api/otp/** — no conflict with the main SecurityConfig
    @Bean
    public SecurityFilterChain permitOtpOnly(HttpSecurity http) throws Exception {
        http
          .securityMatcher("/api/otp/**")            // important: limits this chain
          .csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(auth -> auth
              .anyRequest().permitAll()
          );
        return http.build();
    }
}
