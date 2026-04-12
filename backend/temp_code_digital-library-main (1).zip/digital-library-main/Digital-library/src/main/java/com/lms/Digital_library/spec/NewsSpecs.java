package com.lms.Digital_library.spec;

import com.lms.Digital_library.entity.NewsArticle;
import org.springframework.data.jpa.domain.Specification;

public class NewsSpecs {

    public static Specification<NewsArticle> keyword(String q) {
        return (root, cq, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("summary")), like),
                    cb.like(cb.lower(root.get("tags")), like),
                    cb.like(cb.lower(root.get("pub")), like)
            );
        };
    }

    public static Specification<NewsArticle> publication(String pub) {
        return (root, cq, cb) -> (pub == null || pub.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("pub"), pub);
    }

    public static Specification<NewsArticle> year(Integer year) {
        return (root, cq, cb) -> (year == null)
                ? cb.conjunction()
                : cb.equal(root.get("year"), year);
    }

    public static Specification<NewsArticle> type(String type) {
        return (root, cq, cb) -> (type == null || type.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("type"), type);
    }
}
