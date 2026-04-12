package com.lms.Digital_library.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
public class YouTubeService {

    @Value("${youtube.api.key}")
    private String apiKey;

    public String searchVideos(String query) {
        String url = "https://www.googleapis.com/youtube/v3/search"
                + "?part=snippet"
                + "&q=" + query
                + "&type=video"
                + "&maxResults=10"
                + "&key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response.getBody();
    }
}
