package com.lms.Digital_library.dto;

import com.lms.Digital_library.entity.NewsArticle;

public record NewsDto(
        Long id, String title, String summary,
        String pub, Integer year, String type, String tags,
        String url, String downloadUrl, Integer views
) {
    public static NewsDto of(NewsArticle n) {
        return new NewsDto(
                n.getId(), n.getTitle(), n.getSummary(),
                n.getPub(), n.getYear(), n.getType(), n.getTags(),
                n.getUrl(), n.getDownloadUrl(), n.getViews()
        );
    }
}
