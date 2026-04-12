package com.lms.Digital_library.dto;

import com.lms.Digital_library.entity.DigitalResource;

public record DigitalResourceDTO(
        Long id, String title, String description,
        DigitalResource.Category category,
        String tags, String authors, Integer year,
        String provider, String sourceUrl, String downloadUrl, String thumbnailUrl
) {
    public static DigitalResourceDTO from(DigitalResource r){
        return new DigitalResourceDTO(
            r.getId(), r.getTitle(), r.getDescription(),
            r.getCategory(), r.getTags(), r.getAuthors(), r.getYear(),
            r.getProvider(), r.getSourceUrl(), r.getDownloadUrl(), r.getThumbnailUrl()
        );
    }
}
