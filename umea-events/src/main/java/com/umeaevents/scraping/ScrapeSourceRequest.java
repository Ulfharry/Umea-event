package com.umeaevents.scraping;

import jakarta.validation.constraints.NotBlank;

/**
 * Create request for a scrape source.
 *
 * @param enabled optional; defaults to true when omitted
 */
public record ScrapeSourceRequest(
        @NotBlank String name,
        @NotBlank String sitemapUrl,
        @NotBlank String urlPattern,
        Boolean enabled
) {}
