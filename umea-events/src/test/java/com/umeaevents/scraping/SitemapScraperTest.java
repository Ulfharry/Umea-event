package com.umeaevents.scraping;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

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
