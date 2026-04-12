package com.lms.Digital_library.service;

import com.lms.Digital_library.entity.ResearchPaper;
import com.lms.Digital_library.repository.ResearchRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@Service
public class ResearchService {

    private final ResearchRepository repo;
    private final FileStorageService storage;

    public ResearchService(ResearchRepository repo, FileStorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    public ResearchPaper uploadPaper(String title, String authors, Integer year, String tags,
                                     MultipartFile file, String uploadedBy) throws IOException {
        String storedName = storage.store(file);

        ResearchPaper paper = new ResearchPaper();
        paper.setTitle(title);
        paper.setAuthors(authors);
        paper.setYear(year);
        paper.setTags(tags);
        paper.setFilename(storedName);
        paper.setOriginalFilename(file.getOriginalFilename());
        paper.setContentType(file.getContentType());
        paper.setSizeBytes(file.getSize());
        paper.setUploadedAt(Instant.now());
        paper.setUploadedBy(uploadedBy); // may be null

        return repo.save(paper);
    }

    public Page<ResearchPaper> search(String q, int page, int size, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, pageableSortOrDefault(sort));
        if (q == null || q.isBlank()) {
            return repo.findAll(pageable);
        }
        String q2 = q.trim();
        return repo.findByTitleContainingIgnoreCaseOrAuthorsContainingIgnoreCaseOrTagsContainingIgnoreCase(q2, q2, q2, pageable);
    }

    public Optional<ResearchPaper> findById(Long id) {
        return repo.findById(id);
    }

    public Path getFilePath(String filename) {
        return storage.load(filename);
    }

    public void deleteById(Long id) throws IOException {
        repo.findById(id).ifPresent(p -> {
            try {
                storage.delete(p.getFilename());
            } catch (IOException e) {
                // log but continue to delete DB record
            }
            repo.deleteById(id);
        });
    }

    public Page<ResearchPaper> findByUploader(String uploadedBy, int page, int size, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, pageableSortOrDefault(sort));
        return repo.findByUploadedBy(uploadedBy, pageable);
    }

    private Sort pageableSortOrDefault(Sort sort) {
        if (sort != null && sort.isSorted()) return sort;
        return Sort.by(Sort.Direction.DESC, "uploadedAt");
    }
}
