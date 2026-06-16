package com.umeaevents.scraping;

import jakarta.validation.constraints.NotBlank;

/**
 * Request for sitemap-driven scraping.
 *
 * @param sitemapUrl absolute URL of an XML sitemap (e.g. https://olearys.com/sitemap/se.xml)
 * @param urlPattern Java regex matched (via find) against each {@code <loc>} URL; only matching
 *                   URLs are fetched. Use it to keep just event detail pages and drop the bare
 *                   listing page, e.g. {@code /sv-se/umeaa/events/.+}
 */
public record SitemapScrapeRequest(
        @NotBlank String sitemapUrl,
        @NotBlank String urlPattern
) {}
