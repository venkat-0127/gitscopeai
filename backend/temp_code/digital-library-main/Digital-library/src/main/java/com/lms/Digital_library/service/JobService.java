package com.lms.Digital_library.service;

import com.lms.Digital_library.entity.Job;
import com.lms.Digital_library.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class JobService {
    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public List<Job> listAll() { return jobRepository.findAll(); }

    public Optional<Job> findById(Long id) { return jobRepository.findById(id); }

    @Transactional
    public Job upsertBySource(String source, String sourceId, Job incoming) {
        Optional<Job> existing = jobRepository.findBySourceAndSourceId(source, sourceId);
        if(existing.isPresent()) {
            Job j = existing.get();
            j.setTitle(incoming.getTitle());
            j.setCompany(incoming.getCompany());
            j.setLocation(incoming.getLocation());
            j.setDescription(incoming.getDescription());
            j.setApplyUrl(incoming.getApplyUrl());
            j.setJobType(incoming.getJobType());
            j.setRemoteType(incoming.getRemoteType());
            j.setPostedDate(incoming.getPostedDate());
            j.setDeadline(incoming.getDeadline());
            j.setFetchedAt(LocalDateTime.now());
            return jobRepository.save(j);
        } else {
            incoming.setSource(source);
            incoming.setSourceId(sourceId);
            incoming.setFetchedAt(LocalDateTime.now());
            return jobRepository.save(incoming);
        }
    }

    public Job create(Job job) {
        job.setFetchedAt(LocalDateTime.now());
        return jobRepository.save(job);
    }
}
