package com.lms.Digital_library.controller;

import com.lms.Digital_library.dto.JobDTO;
import com.lms.Digital_library.entity.Job;
import com.lms.Digital_library.entity.JobApplication;
import com.lms.Digital_library.repository.JobApplicationRepository;
import com.lms.Digital_library.repository.JobRepository;
import com.lms.Digital_library.service.JobIngestService;
import com.lms.Digital_library.service.JobService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*") // restrict in production
public class JobController {

    private final JobService jobService;
    private final JobRepository jobRepository;
    private final JobIngestService ingestService;
    private final JobApplicationRepository appRepo;

    public JobController(JobService jobService,
                         JobRepository jobRepository,
                         JobIngestService ingestService,
                         JobApplicationRepository appRepo) {
        this.jobService = jobService;
        this.jobRepository = jobRepository;
        this.ingestService = ingestService;
        this.appRepo = appRepo;
    }

    // --- Mapper helper ---
    private JobDTO toDto(Job j) {
        return new JobDTO(
                j.getId(),
                j.getTitle(),
                j.getCompany(),
                j.getLocation(),
                j.getApplyUrl(),
                j.getPostedDate(),
                j.getJobType()
        );
    }

    // --- List jobs (returns DTOs) ---
    @GetMapping
    public Page<JobDTO> list(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(defaultValue = "") String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fetchedAt"));
        Page<Job> jobs;
        if (q == null || q.isBlank()) {
            jobs = jobRepository.findAll(pageable);
        } else {
            jobs = jobRepository.findByTitleContainingIgnoreCaseOrCompanyContainingIgnoreCaseOrLocationContainingIgnoreCase(
                    q, q, q, pageable);
        }
        return jobs.map(this::toDto);
    }

    // --- Internships (returns DTOs) ---
    @GetMapping("/internships")
    public Page<JobDTO> internships(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(defaultValue = "") String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fetchedAt"));
        Page<Job> jobs;
        if (q == null || q.isBlank()) {
            jobs = jobRepository.findByJobTypeIgnoreCase("Intern", pageable);
            return jobs.map(this::toDto);
        }
        Page<Job> filtered = jobRepository.findByTitleContainingIgnoreCaseOrCompanyContainingIgnoreCaseOrLocationContainingIgnoreCase(
                q, q, q, pageable);
        List<Job> internList = filtered.stream()
                .filter(j -> {
                    String jt = j.getJobType() == null ? "" : j.getJobType();
                    String title = j.getTitle() == null ? "" : j.getTitle();
                    return "Intern".equalsIgnoreCase(jt) || title.toLowerCase().contains("intern");
                }).collect(Collectors.toList());

        return new PageImpl<>(internList.stream().map(this::toDto).collect(Collectors.toList()), pageable, internList.size());
    }

    // --- Get single job by id (entity) ---
    @GetMapping("/{id}")
    public ResponseEntity<JobDTO> getJob(@PathVariable Long id) {
        Optional<Job> job = jobRepository.findById(id);
        return job.map(j -> ResponseEntity.ok(this.toDto(j)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // --- Create a new job (manual add) ---
    @PostMapping
    public ResponseEntity<JobDTO> add(@RequestBody Job job) {
        Job created = jobService.create(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.toDto(created));
    }

    // --- Trigger immediate sync from Adzuna ---
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncNow() {
        new Thread(() -> {
            try {
                ingestService.ingestFromAdzuna();
            } catch (Exception e) {
                // service logs normally; no-op here
            }
        }).start();
        return ResponseEntity.ok(Map.of("message", "Sync started"));
    }

    // --- Receive job application submissions ---
    @PostMapping("/apply")
    public ResponseEntity<Map<String, String>> apply(@RequestBody JobApplication application) {
        appRepo.save(application);
        return ResponseEntity.ok(Map.of("message", "Application received"));
    }

    // --- Optional: delete a job (admin) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteJob(@PathVariable Long id) {
        if (!jobRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Job not found"));
        }
        jobRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}
