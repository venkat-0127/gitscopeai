package com.lms.Digital_library.dto;

import com.lms.Digital_library.entity.ProjectItem;

public class ProjectItemDTO {
    public Long id;
    public String title;
    public String owner;
    public String techStack;
    public Integer year;
    public String githubUrl;
    public String downloadUrl;
    public String description;
    public String category;

    public static ProjectItemDTO from(ProjectItem p){
        ProjectItemDTO d = new ProjectItemDTO();
        d.id = p.getId();
        d.title = p.getTitle();
        d.owner = p.getOwner();
        d.techStack = p.getTechStack();
        d.year = p.getYear();
        d.githubUrl = p.getGithubUrl();
        d.downloadUrl = p.getDownloadUrl();
        d.description = p.getDescription();
        d.category = (p.getCategory() != null) ? p.getCategory().name() : null;
        return d;
    }
}
