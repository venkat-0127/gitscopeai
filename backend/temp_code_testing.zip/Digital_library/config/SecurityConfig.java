package com.lms.Digital_library.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * ✅ Optional GitHub OAuth2 success handler — only if you actually define one.
     * It’s marked @Autowired(required = false) so that the app still runs even
     * when no handler bean is present.
     */
    @Autowired(required = false)
    @Nullable
    private AuthenticationSuccessHandler githubOAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // ---- CORS ----
        http.cors(Customizer.withDefaults());

        // ---- CSRF ----
        // We keep CSRF cookies for browser pages, but ignore /api/** for convenience.
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(request -> {
                    String uri = request.getRequestURI();
                    return uri != null && uri.startsWith("/api/");
                })
            );

        // ---- AUTH RULES (still effectively "open" so nothing breaks) ----
        http
            .authorizeHttpRequests(auth -> auth
                // Pre-flight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public API endpoints
                .requestMatchers("/api/otp/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/research/**").permitAll()
                .requestMatchers("/api/resumes/**").permitAll()

                // Explicitly mark admin & librarian login as public
                .requestMatchers("/api/admin/login", "/api/librarian/login").permitAll()

                // Static pages / assets
                .requestMatchers(
                        "/", "/index.html",
                        "/resume_builder.html", "/resume_builder.css", "/resume_builder.js",
                        "/research.html",
                        "/favicon.ico",
                        "/css/**", "/js/**", "/images/**", "/webjars/**",
                        "/main.html"
                ).permitAll()

                // 🚨 IMPORTANT:
                // keep everything else open for now because your own controllers
                // (AdminController, LibrarianAdminController, etc.) are handling
                // DB checks and returning 401/403 manually.
                .anyRequest().permitAll()
            );

        // ---- OAuth2 (optional, non-blocking) ----
        if (githubOAuth2SuccessHandler != null) {
            http.oauth2Login(oauth -> oauth.successHandler((request, response, authentication) -> {
                // simple redirect after OAuth login
                response.sendRedirect("/");
            }));
        } else {
            http.oauth2Login(Customizer.withDefaults());
        }

        // ---- Disable formLogin & httpBasic so you don’t get browser popups ----
        http.formLogin(form -> form.disable());
        // DO NOT enable httpBasic(), to avoid basic-auth popups.

        return http.build();
    }

    /**
     * Very open CORS config for local development.
     * In production, restrict allowed origins.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
                "http://localhost:5500",
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8080"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * In-memory users kept only for convenience in dev.
     * They won’t be used for your admin / librarian JSON logins,
     * because formLogin & httpBasic are disabled.
     */
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
        // cost 12 is a reasonable default for development; adjust later if needed
        return new BCryptPasswordEncoder();
    }
}
