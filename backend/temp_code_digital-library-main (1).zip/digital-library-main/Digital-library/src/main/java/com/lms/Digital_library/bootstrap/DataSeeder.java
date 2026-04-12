package com.lms.Digital_library.bootstrap;

import com.lms.Digital_library.entity.NewsArticle;
import com.lms.Digital_library.repository.NewsRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private final NewsRepository repo;

    public DataSeeder(NewsRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        // 👇 prevent duplicate inserts
        if (repo.count() > 0) return;

        String[] pubs = {"The Hindu", "Times of India", "Indian Express", "The Economic Times", "The Telegraph"};
        String[] types = {"epaper", "editorials", "clippings", "govt", "current"};
        Random rnd = new Random();

        for (int i = 1; i <= 40; i++) {
            NewsArticle n = new NewsArticle();
            n.setTitle(pubs[i % pubs.length] + " — Sample Article " + i);
            n.setSummary("This is a sample newspaper summary for article " + i + ".");
            n.setPub(pubs[i % pubs.length]);
            n.setYear(2025 - (i % 4));
            n.setType(types[i % types.length]);
            n.setTags("education,technology,politics");
            n.setUrl("https://example.com/view/" + i);
            n.setDownloadUrl("https://example.com/pdf/" + i + ".pdf");
            n.setViews(100 + rnd.nextInt(900));

            repo.save(n);
        }

        System.out.println("✅ Sample newspaper data seeded successfully.");
    }
}
