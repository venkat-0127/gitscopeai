package com.lms.Digital_library.service;

import com.lms.Digital_library.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
public class JobIngestService {

    private static final Logger log = LoggerFactory.getLogger(JobIngestService.class);

    private final JobService jobService;
    private final RestTemplate restTemplate;

    @Value("${adzuna.app_id:}")
    private String adzAppId;

    @Value("${adzuna.app_key:}")
    private String adzAppKey;

    @Value("${adzuna.country:us}")
    private String adzCountry;

    @Value("${adzuna.results_per_page:50}")
    private int resultsPerPage;

    public JobIngestService(JobService jobService, RestTemplate restTemplate) {
        this.jobService = jobService;
        this.restTemplate = restTemplate;
    }

    /**
     * Scheduled ingest. Interval controlled by career.ingest.delay (ISO-8601 format e.g. PT30M)
     */
    @Scheduled(fixedDelayString = "${career.ingest.delay:PT30M}")
    public void scheduledIngest() {
        try {
            ingestFromAdzuna();
        } catch (Exception ex) {
            log.error("Unexpected error during scheduledIngest: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Calls Adzuna direct API and upserts results via jobService.
     */
    public void ingestFromAdzuna() {
        if (adzAppId == null || adzAppId.isBlank() || adzAppKey == null || adzAppKey.isBlank()) {
            log.warn("Adzuna credentials missing (adzuna.app_id / adzuna.app_key). Skipping ingest.");
            return;
        }

        String url = "https://api.adzuna.com/v1/api/jobs/" + adzCountry +
                "/search/1?app_id=" + adzAppId +
                "&app_key=" + adzAppKey +
                "&results_per_page=" + resultsPerPage;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {
            };

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(url, HttpMethod.GET, entity, typeRef);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.warn("Adzuna ingest returned non-OK status {} for url={}", resp.getStatusCode(), url);
                return;
            }

            Map<String, Object> body = resp.getBody();
            if (body == null) {
                log.warn("Adzuna response body was null for url={}", url);
                return;
            }

            Object resultsObj = body.get("results");
            if (!(resultsObj instanceof List<?> resultsList)) {
                log.warn("Adzuna response 'results' is not a list (class={}) for url={}",
                        resultsObj == null ? "null" : resultsObj.getClass().getName(), url);
                return;
            }

            for (Object itemObj : resultsList) {
                if (!(itemObj instanceof Map<?, ?> rawMap)) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) rawMap;

                String id = String.valueOf(item.getOrDefault("id", item.get("ad_id")));
                if (id == null || id.isBlank()) {
                    // skip items without an id
                    continue;
                }

                Job j = new Job();
                j.setTitle(String.valueOf(item.getOrDefault("title", "")));
                j.setDescription(String.valueOf(item.getOrDefault("description", "")));

                Object redirect = item.getOrDefault("redirect_url", item.getOrDefault("url", ""));
                j.setApplyUrl(redirect == null ? "" : String.valueOf(redirect));

                // job type detection
                String titleLower = j.getTitle() == null ? "" : j.getTitle().toLowerCase();
                String descLower = j.getDescription() == null ? "" : j.getDescription().toLowerCase();
                if (titleLower.contains("intern") || descLower.contains("intern")) {
                    j.setJobType("Intern");
                } else {
                    Object contractType = item.get("contract_type");
                    if (contractType != null) j.setJobType(String.valueOf(contractType));
                }

                // company
                Object cmpObj = item.get("company");
                if (cmpObj instanceof Map<?, ?> cmpMap) {
                    Object cname = ((Map<?, ?>) cmpMap).get("display_name");
                    if (cname != null) j.setCompany(String.valueOf(cname));
                } else if (item.get("company") != null) {
                    j.setCompany(String.valueOf(item.get("company")));
                }

                // location
                Object locObj = item.get("location");
                if (locObj instanceof Map<?, ?> locMap) {
                    Object lname = ((Map<?, ?>) locMap).get("display_name");
                    if (lname != null) j.setLocation(String.valueOf(lname));
                } else if (item.get("location") != null) {
                    j.setLocation(String.valueOf(item.get("location")));
                }

                // posted date (best-effort)
                Object createdObj = item.get("created");
                if (createdObj instanceof String created) {
                    LocalDate parsedDate = tryParseDate(created);
                    if (parsedDate != null) j.setPostedDate(parsedDate);
                }

                j.setSource("adzuna");
                j.setSourceId(id);
                j.setFetchedAt(LocalDateTime.now());

                try {
                    jobService.upsertBySource("adzuna", id, j);
                } catch (Exception upsertEx) {
                    log.warn("Failed to upsert job id={} : {}", id, upsertEx.getMessage());
                }
            }

        } catch (HttpClientErrorException.NotFound nf) {
            log.warn("Adzuna endpoint not found (404) for url={}. Message: {}", url, nf.getResponseBodyAsString());
        } catch (HttpClientErrorException.Unauthorized ua) {
            log.warn("Adzuna unauthorized (401) for url={}. Message: {}", url, ua.getResponseBodyAsString());
        } catch (HttpClientErrorException hce) {
            log.warn("Adzuna HTTP error {} for url={}. Body: {}", hce.getStatusCode(), url, hce.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error while ingesting from Adzuna: {}", e.getMessage(), e);
        }
    }

    /**
     * Best-effort parse of date strings into LocalDate.
     */
    private LocalDate tryParseDate(String input) {
        if (input == null || input.isBlank()) return null;

        try {
            return LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(input, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }

        // fallback: first 10 chars often YYYY-MM-DD
        if (input.length() >= 10) {
            try {
                return LocalDate.parse(input.substring(0, 10));
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
