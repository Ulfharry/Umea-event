package com.umeaevents.scraping;

import java.util.List;

/**
 * Extension point for adding scraping sources.
 * Implement this interface and register as a Spring @Component to add a new source.
 * The scraper runner (future M7+) will discover all adapters via Spring injection.
 *
 * IMPORTANT: scraped events must NEVER be auto-published.
 * All data returned by scrape() goes into raw_scraped_event with status PENDING_REVIEW
 * and must be explicitly promoted by an ADMIN.
 */
public interface ScraperAdapter {

    /** Unique identifier for this source, stored as raw_scraped_event.source. */
    ScrapedEventSource sourceName();

    /** Fetch raw events from the external source. */
    List<RawScrapedEvent> scrape();
}
