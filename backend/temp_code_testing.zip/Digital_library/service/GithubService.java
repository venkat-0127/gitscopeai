package com.lms.Digital_library.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GithubService {
    private final RestTemplate rest = new RestTemplate();

    /**
     * Fetches the authenticated user's repos from GitHub.
     * Uses ParameterizedTypeReference to preserve generics and avoid raw-type warnings.
     */
    public List<Map<String, Object>> listUserRepos(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null) headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Accept", "application/vnd.github.v3+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ParameterizedTypeReference<List<Map<String, Object>>> typeRef =
                new ParameterizedTypeReference<>() {};

        ResponseEntity<List<Map<String, Object>>> resp = rest.exchange(
                "https://api.github.com/user/repos?per_page=100",
                HttpMethod.GET,
                entity,
                typeRef
        );

        return resp.getBody();
    }

    /**
     * Fetch metadata for a specific repo.
     * Uses ParameterizedTypeReference to avoid raw types.
     */
    public Map<String, Object> getRepo(String owner, String repo, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null) headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Accept", "application/vnd.github.v3+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ParameterizedTypeReference<Map<String, Object>> typeRef =
                new ParameterizedTypeReference<>() {};

        ResponseEntity<Map<String, Object>> resp = rest.exchange(
                "https://api.github.com/repos/" + owner + "/" + repo,
                HttpMethod.GET,
                entity,
                typeRef
        );

        return resp.getBody();
    }
}
