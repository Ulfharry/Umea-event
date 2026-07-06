package com.umeaevents.scraping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Create/update request for a scrape source.
 *
 * @param enabled     optional; defaults to true when omitted
 * @param maxAgeDays  optional freshness filter: skip sitemap URLs whose lastmod is older than this
 *                    many days. Null = no filtering.
 */
public record ScrapeSourceRequest(
        @NotBlank String name,
        @NotBlank String sitemapUrl,
        @NotBlank String urlPattern,
        Boolean enabled,
        @Positive Integer maxAgeDays
) {}
