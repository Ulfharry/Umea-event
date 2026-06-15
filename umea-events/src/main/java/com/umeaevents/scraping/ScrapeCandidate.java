package com.umeaevents.scraping;

import java.time.OffsetDateTime;

/**
 * Intermediate result from a scraper adapter — pure data, no JPA.
 * The service layer converts these into RawScrapedEvent entities.
 */
public record ScrapeCandidate(
        String title,
        String description,
        String rawDateText,
        String sourceUrl,
        OffsetDateTime scrapedAt
) {}
