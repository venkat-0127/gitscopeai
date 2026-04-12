package com.lms.Digital_library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, GithubOAuth2SuccessHandler githubOAuth2SuccessHandler) throws Exception {
        http
            // disable CSRF for API testing (enable appropriately in production)
            .csrf(csrf -> csrf.disable())

            // authorize requests
            .authorizeHttpRequests(auth -> auth
                // OTP endpoints must be public for your frontend flow
                .requestMatchers("/api/otp/**").permitAll()

                // allow public GET for research listing & downloads
                .requestMatchers(HttpMethod.GET, "/api/research/**").permitAll()

                // allow static frontend files
                .requestMatchers("/", "/index.html", "/research.html", "/favicon.ico",
                                 "/css/**", "/js/**", "/images/**", "/webjars/**",
                                 "/main.html").permitAll()

                // allow everything else during demo — prevents login popup
                .anyRequest().permitAll()
            )

            // enable GitHub OAuth2 login
            .oauth2Login(oauth -> oauth
                .successHandler(githubOAuth2SuccessHandler)
            )

            // disable default login forms
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder passwordEncoder) {
        InMemoryUserDetailsManager mgr = new InMemoryUserDetailsManager();

        mgr.createUser(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("adminpass"))
                .roles("ADMIN", "USER")
                .build());

        mgr.createUser(User.builder()
                .username("user")
                .password(passwordEncoder.encode("userpass"))
                .roles("USER")
                .build());

        return mgr;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
