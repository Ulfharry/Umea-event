package com.umeaevents.scraping;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HTML parsing logic — no network, no Spring context.
 * Uses Jsoup.parse() to inject pre-built HTML directly into extractCandidates().
 */
class JsoupHtmlScraperTest {

    private static final String SOURCE_URL = "https://test.example/events";
    private final JsoupHtmlScraper scraper = new JsoupHtmlScraper();

    @Test
    void articleContainers_extractsTitleAndDescription() {
        var html = """
                <html><body>
                  <article><h2>Pubquiz</h2><p>Rolig kväll med frågor</p></article>
                  <article><h2>Livemusik</h2><p>Jazz på scen</p></article>
                </body></html>
                """;
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).title()).isEqualTo("Pubquiz");
        assertThat(candidates.get(0).description()).isEqualTo("Rolig kväll med frågor");
        assertThat(candidates.get(1).title()).isEqualTo("Livemusik");
    }

    @Test
    void articleContainers_withDateElement_extractsRawDateText() {
        var html = """
                <html><body>
                  <article>
                    <h2>Standup-kväll</h2>
                    <time datetime="2026-08-15">15 aug 2026</time>
                    <p>Tre komiker</p>
                  </article>
                  <article>
                    <h2>DJ-kväll</h2>
                    <time datetime="2026-08-22">22 aug 2026</time>
                  </article>
                </body></html>
                """;
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).rawDateText()).isEqualTo("2026-08-15");
        assertThat(candidates.get(1).rawDateText()).isEqualTo("2026-08-22");
    }

    @Test
    void articleContainers_dateInText_extractsViaRegex() {
        var html = """
                <html><body>
                  <article><h2>Event A</h2><p>Datum: 14 jun</p></article>
                  <article><h2>Event B</h2><p>Datum: 2026-07-01</p></article>
                </body></html>
                """;
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates.get(0).rawDateText()).isEqualTo("14 jun");
        assertThat(candidates.get(1).rawDateText()).isEqualTo("2026-07-01");
    }

    @Test
    void noContainers_fallsBackToHeadings() {
        var html = """
                <html><body>
                  <h2>Konsert</h2><p>Bra band spelar</p>
                  <h2>Teater</h2><p>Spännande pjäs</p>
                </body></html>
                """;
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).title()).isEqualTo("Konsert");
        assertThat(candidates.get(0).description()).isEqualTo("Bra band spelar");
        assertThat(candidates.get(1).title()).isEqualTo("Teater");
    }

    @Test
    void emptyPage_returnsEmptyList() {
        var html = "<html><body><p>Inga event just nu</p></body></html>";
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates).isEmpty();
    }

    @Test
    void blankTitles_areFiltered() {
        var html = """
                <html><body>
                  <article><h2>  </h2><p>Ingen titel</p></article>
                  <article><h2>Riktig titel</h2><p>Med innehåll</p></article>
                </body></html>
                """;
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).title()).isEqualTo("Riktig titel");
    }

    @Test
    void allCandidates_haveCorrectSourceUrl() {
        var html = """
                <html><body>
                  <article><h2>Event</h2><p>Beskrivning ett</p></article>
                  <article><h2>Event 2</h2><p>Beskrivning två</p></article>
                </body></html>
                """;
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates).hasSize(2);
        assertThat(candidates).allMatch(c -> SOURCE_URL.equals(c.sourceUrl()));
    }

    @Test
    void allCandidates_haveScrapedAtTimestamp() {
        var html = """
                <html><body>
                  <article><h2>Event</h2><p>Beskrivning ett</p></article>
                  <article><h2>Event 2</h2><p>Beskrivning två</p></article>
                </body></html>
                """;
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates).allMatch(c -> c.scrapedAt() != null);
    }

    // ── junk filter ───────────────────────────────────────────────────────────

    @Test
    void titleOnly_candidatesAreFilteredOut() {
        // Real-world case: a selector matches layout wrappers that all share the same page
        // heading, producing many candidates with a title but no description and no date.
        var html = """
                <html><body>
                  <div><h2>Event</h2><p>QUIZ varje onsdag, kom och spela!</p></div>
                  <div><h2>Event</h2></div>
                  <div><h2>Event</h2></div>
                  <div><h2>Event</h2></div>
                </body></html>
                """;
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).description()).isEqualTo("QUIZ varje onsdag, kom och spela!");
    }

    @Test
    void candidateWithDateButNoDescription_isKept() {
        var html = """
                <html><body>
                  <article><h2>Livemusik</h2><time datetime="2026-09-01">1 sep</time></article>
                  <article><h2>Tom rad</h2></article>
                </body></html>
                """;
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).title()).isEqualTo("Livemusik");
        assertThat(candidates.get(0).rawDateText()).isEqualTo("2026-09-01");
    }

    @Test
    void duplicateCandidates_areCollapsed() {
        var html = """
                <html><body>
                  <article><h2>Pubquiz</h2><p>Samma text</p></article>
                  <article><h2>Pubquiz</h2><p>Samma text</p></article>
                  <article><h2>Annat</h2><p>Annan text</p></article>
                </body></html>
                """;
        var candidates = scraper.extractCandidates(Jsoup.parse(html, SOURCE_URL), SOURCE_URL);

        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(ScrapeCandidate::title)
                .containsExactly("Pubquiz", "Annat");
    }
}
