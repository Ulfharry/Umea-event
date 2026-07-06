package com.umeaevents.scraping;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for sitemap parsing, URL filtering, and detail-page extraction — no network.
 */
class SitemapScraperTest {

    private static final String DETAIL_URL = "https://venue.example/sv-se/umeaa/events/musikquiz/";
    private final SitemapScraper scraper = new SitemapScraper();

    private static final String SITEMAP_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
              <url><loc>https://venue.example/sv-se/umeaa/</loc></url>
              <url><loc>https://venue.example/sv-se/umeaa/events/</loc></url>
              <url><loc>https://venue.example/sv-se/umeaa/events/musikquiz/</loc></url>
              <url><loc>https://venue.example/sv-se/umeaa/events/live-band/</loc></url>
              <url><loc>https://venue.example/sv-se/umeaa/food/lunch/</loc></url>
            </urlset>
            """;

    // ── sitemap parsing ─────────────────────────────────────────────────────────

    @Test
    void extractLocs_returnsAllLocValues() {
        var doc = Jsoup.parse(SITEMAP_XML, "", Parser.xmlParser());
        var locs = scraper.extractLocs(doc);

        assertThat(locs).hasSize(5);
        assertThat(locs).contains("https://venue.example/sv-se/umeaa/events/musikquiz/");
    }

    // ── URL filtering ───────────────────────────────────────────────────────────

    @Test
    void filterUrls_keepsOnlyEventDetailPages() {
        var doc = Jsoup.parse(SITEMAP_XML, "", Parser.xmlParser());
        var locs = scraper.extractLocs(doc);

        // .+ after events/ excludes the bare listing page and non-event pages
        var filtered = scraper.filterUrls(locs, "/sv-se/umeaa/events/.+");

        assertThat(filtered).containsExactly(
                "https://venue.example/sv-se/umeaa/events/musikquiz/",
                "https://venue.example/sv-se/umeaa/events/live-band/"
        );
    }

    @Test
    void filterUrls_dropsDuplicates() {
        var filtered = scraper.filterUrls(
                List.of("https://x/events/a/", "https://x/events/a/", "https://x/events/b/"),
                "/events/.+");

        assertThat(filtered).containsExactly("https://x/events/a/", "https://x/events/b/");
    }

    // ── lastmod freshness filtering ─────────────────────────────────────────────

    private static final String SITEMAP_WITH_LASTMOD = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
              <url><loc>https://x/events/fresh/</loc><lastmod>2026-06-15T10:14:33.543Z</lastmod></url>
              <url><loc>https://x/events/old/</loc><lastmod>2025-01-02T08:00:00.000Z</lastmod></url>
              <url><loc>https://x/events/dateonly/</loc><lastmod>2026-06-20</lastmod></url>
              <url><loc>https://x/events/nomod/</loc></url>
            </urlset>
            """;

    @Test
    void parseLastmod_handlesFullInstantAndDateOnly() {
        assertThat(SitemapScraper.parseLastmod("2026-06-15T10:14:33.543Z")).isNotNull();
        assertThat(SitemapScraper.parseLastmod("2026-06-20")).isNotNull();
        assertThat(SitemapScraper.parseLastmod("garbage")).isNull();
        assertThat(SitemapScraper.parseLastmod("")).isNull();
    }

    @Test
    void extractEntries_parsesLocAndLastmod() {
        var doc = Jsoup.parse(SITEMAP_WITH_LASTMOD, "", Parser.xmlParser());
        var entries = scraper.extractEntries(doc);

        assertThat(entries).hasSize(4);
        var nomod = entries.stream().filter(e -> e.loc().endsWith("/nomod/")).findFirst().orElseThrow();
        assertThat(nomod.lastmod()).isNull();
    }

    @Test
    void filterUrls_dropsStaleButKeepsFreshDateOnlyAndUnknown() {
        var doc = Jsoup.parse(SITEMAP_WITH_LASTMOD, "", Parser.xmlParser());
        var entries = scraper.extractEntries(doc);
        var cutoff = OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        var kept = scraper.filterUrls(entries, "/events/.+", cutoff);

        // fresh (Jun 15) + date-only (Jun 20) + unknown lastmod are kept; old (Jan 2025) is dropped
        assertThat(kept).containsExactlyInAnyOrder(
                "https://x/events/fresh/",
                "https://x/events/dateonly/",
                "https://x/events/nomod/");
    }

    @Test
    void filterUrls_nullCutoff_keepsAll() {
        var doc = Jsoup.parse(SITEMAP_WITH_LASTMOD, "", Parser.xmlParser());
        var entries = scraper.extractEntries(doc);

        var kept = scraper.filterUrls(entries, "/events/.+", null);

        assertThat(kept).hasSize(4);
    }

    // ── detail-page extraction ──────────────────────────────────────────────────

    @Test
    void extractDetail_prefersH1AndWysiwygDescription() {
        var html = """
                <html><body>
                  <h1 class="sr-only">Musikquiz Onsdagar</h1>
                  <h2>Musikquiz</h2>
                  <div class="wysiwyg"><p>Varje onsdag startar quizet 20:00. Välkommen!</p></div>
                </body></html>
                """;
        var c = scraper.extractDetail(Jsoup.parse(html, DETAIL_URL), DETAIL_URL);

        assertThat(c).isNotNull();
        assertThat(c.title()).isEqualTo("Musikquiz Onsdagar");
        assertThat(c.description()).isEqualTo("Varje onsdag startar quizet 20:00. Välkommen!");
        assertThat(c.sourceUrl()).isEqualTo(DETAIL_URL);
    }

    @Test
    void extractDetail_extractsDateFromDescriptionText() {
        var html = """
                <html><body>
                  <h1>Live: Tryckförbandet</h1>
                  <div class="wysiwyg"><p>Den 13 december intar bandet scenen!</p></div>
                </body></html>
                """;
        var c = scraper.extractDetail(Jsoup.parse(html, DETAIL_URL), DETAIL_URL);

        assertThat(c.rawDateText()).isEqualTo("13 december");
    }

    @Test
    void extractDetail_fallsBackToMainParagraphWhenNoWysiwyg() {
        var html = """
                <html><body>
                  <h1>Open Mic</h1>
                  <main><p>Onsdagar hela sommaren, start ca 22:00.</p></main>
                </body></html>
                """;
        var c = scraper.extractDetail(Jsoup.parse(html, DETAIL_URL), DETAIL_URL);

        assertThat(c.description()).isEqualTo("Onsdagar hela sommaren, start ca 22:00.");
    }

    @Test
    void extractDetail_returnsNullWhenNoTitle() {
        var html = "<html><head><title></title></head><body><p>No heading</p></body></html>";
        var c = scraper.extractDetail(Jsoup.parse(html, DETAIL_URL), DETAIL_URL);

        assertThat(c).isNull();
    }

    @Test
    void extractDetail_usesDocumentTitleAsLastResort() {
        var html = "<html><head><title>Rednex | Umeå</title></head><body><span>x</span></body></html>";
        var c = scraper.extractDetail(Jsoup.parse(html, DETAIL_URL), DETAIL_URL);

        assertThat(c.title()).isEqualTo("Rednex | Umeå");
    }
}
