package com.lms.Digital_library.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.lms.Digital_library.entity.GithubToken;
import com.lms.Digital_library.repository.GithubTokenRepository;

/**
 * Handles successful GitHub OAuth2 login:
 *  - retrieves the OAuth2AuthorizedClient (to get access token)
 *  - extracts basic user attributes (login / id)
 *  - stores/updates a GithubToken record with the access token
 *  - redirects user back to the frontend dashboard ("/main.html")
 *
 * Paste this file into:
 *   src/main/java/com/lms/Digital_library/config/GithubOAuth2SuccessHandler.java
 */
@Component
public class GithubOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GithubOAuth2SuccessHandler.class);

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private GithubTokenRepository tokenRepo;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            // Not an OAuth2 login (shouldn't happen here) — continue the normal flow
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        // Load the authorized client (contains the access token)
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(registrationId, oauthToken.getName());

        if (client != null && client.getAccessToken() != null) {
            String accessToken = client.getAccessToken().getTokenValue();

            // read attributes returned by GitHub (login, id, avatar_url, email may be null)
            Map<String, Object> attrs = oauthToken.getPrincipal().getAttributes();
            String login = attrs.get("login") != null ? String.valueOf(attrs.get("login")) : null;
            String githubId = attrs.get("id") != null ? String.valueOf(attrs.get("id")) : null;

            // choose a key to associate the token with — prefer login, fallback to github id
            String usernameKey = (login != null && !login.isEmpty()) ? login : githubId;

            if (usernameKey == null) {
                log.warn("GitHub OAuth login returned no 'login' or 'id' attribute; cannot persist token.");
            } else {
                Optional<GithubToken> existing = tokenRepo.findByUsernameAndProvider(usernameKey, "github");

                GithubToken tokenEntity = existing.orElseGet(() -> {
                    GithubToken t = new GithubToken();
                    t.setUsername(usernameKey);
                    t.setProvider("github");
                    return t;
                });

                tokenEntity.setAccessToken(accessToken);
                tokenEntity.setCreatedAt(System.currentTimeMillis());

                tokenRepo.save(tokenEntity);
                log.info("Saved GitHub token for user '{}'", usernameKey);
            }
        } else {
            log.warn("No OAuth2AuthorizedClient or access token found for registration '{}', principal '{}'",
                    registrationId, oauthToken.getName());
        }

        // Redirect to the frontend dashboard (adjust path if your main UI is elsewhere)
        getRedirectStrategy().sendRedirect(request, response, "/main.html");
    }
}
