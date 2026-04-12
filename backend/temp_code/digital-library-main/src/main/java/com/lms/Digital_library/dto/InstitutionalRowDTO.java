package com.lms.Digital_library.dto;

public record InstitutionalRowDTO(
        Long id,
        String title,
        String summary,
        String type,
        String dept,
        String authors,
        Integer year,
        String tags,
        String fileUrl,
        String thumbnailUrl
) {}
