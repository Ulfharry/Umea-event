package com.umeaevents.scraping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScrapeSourceRepository extends JpaRepository<ScrapeSource, UUID> {

    List<ScrapeSource> findByEnabledTrue();

    boolean existsBySitemapUrlAndUrlPattern(String sitemapUrl, String urlPattern);
}
