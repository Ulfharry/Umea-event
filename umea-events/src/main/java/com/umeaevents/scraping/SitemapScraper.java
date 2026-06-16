package com.umeaevents.scraping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Sitemap-driven scraper. Fetches an XML sitemap, keeps the {@code <loc>} URLs matching a
 * caller-supplied pattern, then fetches each of those pages and extracts one event candidate
 * per page (title + description).
 *
 * <p>This is the right approach for JS-rendered sites whose listing page only server-renders a
 * couple of events: the individual detail pages are usually server-rendered in full, and each
 * has a stable URL we can later use as a dedup key ({@code externalId}).
 *
 * <p>Generic and config-free per the project goal of low-touch site onboarding — a new site
 * needs only a sitemap URL and a URL pattern, no bespoke parser.
 */
@Service
public class SitemapScraper {

    private static final String USER_AGENT =
            "UmeaEvents-Bot/1.0 (+https://github.com/Ulfharry/Umea-event)";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_PAGES = 100;

    // Selectors tried in order to find the title / description on a detail page.
    private static final List<String> TITLE_SELECTORS = List.of("h1", "h2");
    private static final List<String> DESCRIPTION_SELECTORS = List.of(".wysiwyg", "main p", "article p");

    /**
     * Fetch the sitemap and every matching detail page, returning one candidate per page that
     * yields a usable title. A single broken detail page is skipped, not fatal.
     *
     * @throws IOException if the sitemap itself cannot be fetched
     */
    public List<ScrapeCandidate> scrape(String sitemapUrl, String urlPattern) throws IOException {
        Document xml = Jsoup.connect(sitemapUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .parser(Parser.xmlParser())
                .get();

        var urls = filterUrls(extractLocs(xml), urlPattern);

        var candidates = new ArrayList<ScrapeCandidate>();
        for (var url : urls) {
            try {
                Document page = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .get();
                var candidate = extractDetail(page, url);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            } catch (IOException e) {
                // One unreachable detail page shouldn't sink the whole run — skip it.
            }
        }
        return candidates;
    }

    // ── package-private, unit-testable without network ──────────────────────────

    /** Extract all {@code <loc>} values from a parsed sitemap document. */
    List<String> extractLocs(Document sitemap) {
        return sitemap.select("loc").stream()
                .map(e -> e.text().strip())
                .filter(s -> !s.isBlank())
                .toList();
    }

    /** Keep distinct URLs matching the pattern (find semantics), capped at {@link #MAX_PAGES}. */
    List<String> filterUrls(List<String> locs, String urlPattern) {
        var pattern = Pattern.compile(urlPattern);
        return locs.stream()
                .filter(u -> pattern.matcher(u).find())
                .distinct()
                .limit(MAX_PAGES)
                .toList();
    }

    /**
     * Extract a single candidate from a detail page. The detail URL becomes {@code sourceUrl}
     * so it can be persisted as a stable dedup key. Returns null if no title can be found.
     */
    ScrapeCandidate extractDetail(Document doc, String url) {
        var title = firstText(doc, TITLE_SELECTORS);
        if (title == null) {
            title = blankToNull(doc.title());
        }
        if (title == null) {
            return null;
        }
        var description = firstText(doc, DESCRIPTION_SELECTORS);
        var dateText = DateText.firstDate(description);
        return new ScrapeCandidate(title, description, dateText, url, OffsetDateTime.now());
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private static String firstText(Document doc, List<String> selectors) {
        for (var selector : selectors) {
            var el = doc.selectFirst(selector);
            if (el != null) {
                var text = el.text().strip();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
