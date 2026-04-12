package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="imported_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectImport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(length=2000)
    private String description;
    private String ownerName;
    private String repoFullName; // owner/repo
    private String repoUrl;
    private Integer year;
    private Long importedByUserId; // optional — depends on your user mapping
}
