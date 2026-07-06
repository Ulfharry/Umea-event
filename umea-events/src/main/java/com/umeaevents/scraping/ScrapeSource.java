package com.umeaevents.scraping;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A configured website to scrape on a schedule. The weekly job runs each enabled source's
 * sitemap through {@link SitemapScraper}. Adding a new site is just inserting a row.
 */
@Entity
@Table(name = "scrape_source")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sitemapUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String urlPattern;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * Freshness filter: skip sitemap URLs whose {@code <lastmod>} is older than this many days.
     * NULL means no filtering.
     */
    private Integer maxAgeDays;

    private OffsetDateTime lastRunAt;

    private Integer lastRunNewCount;

    @Column(columnDefinition = "TEXT")
    private String lastRunError;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
