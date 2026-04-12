package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "institutional_items",
       indexes = {
         @Index(name = "idx_ir_title", columnList = "title"),
         @Index(name = "idx_ir_type",  columnList = "type"),
         @Index(name = "idx_ir_year",  columnList = "year"),
         @Index(name = "idx_ir_dept",  columnList = "department")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionalItem {

    public enum Type {
        PROJECT_REPORT, THESIS, SYLLABUS, SEMINAR, PUBLICATION, DIGITAL_RESOURCE, HISTORY_BOOK, OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Type type = Type.OTHER;  // ✅ default retained when using @Builder

    private String department;   // shown as "Dept" in UI
    private String authors;
    private Integer year;
    private String tags;         // comma-separated

    // files / media
    private String downloadUrl;
    private String thumbnailUrl;

    // meta
    private String addedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();  // ✅ default creation timestamp

    /**
     * Fallback safety: ensures default values are set even if builder isn't used.
     */
    @PrePersist
    public void prePersistDefaults() {
        if (type == null) type = Type.OTHER;
        if (createdAt == null) createdAt = Instant.now();
    }
}
