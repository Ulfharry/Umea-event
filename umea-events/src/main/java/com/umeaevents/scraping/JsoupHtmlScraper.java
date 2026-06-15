package com.umeaevents.scraping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Fetches a public HTML page and extracts event-like content as ScrapeCandidate POJOs.
 * Not a ScraperAdapter implementation — this is for on-demand URL testing.
 * The caller (AdminScraperController) decides what to do with the results.
 */
@Service
public class JsoupHtmlScraper {

    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_CANDIDATES = 50;

    // CSS selectors tried in order. First that yields ≥2 elements wins.
    private static final List<String> CONTAINER_SELECTORS = List.of(
            "article",
            "[class*=event]",
            "[class*=Event]",
            "li:has(h2, h3, h4)",
            "div:has(h2):not(header):not(footer):not(nav)",
            "section:has(h2, h3)"
    );

    // Matches common date formats in Swedish and ISO
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(\\d{4}-\\d{2}-\\d{2}" +
            "|\\d{1,2}[./]\\d{1,2}([./]\\d{2,4})?" +
            "|\\d{1,2}\\s+(jan|feb|mar|apr|maj|jun|jul|aug|sep|okt|nov|dec)\\w*)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Fetch the given URL and return extracted event candidates.
     * Never persists anything — caller is responsible for saving.
     *
     * @throws IOException if the URL is unreachable or returns a non-200 response
     */
    public List<ScrapeCandidate> scrape(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("UmeaEvents-Bot/1.0 (+https://github.com/Ulfharry/Umea-event)")
                .timeout(TIMEOUT_MS)
                .get();
        return extractCandidates(doc, url);
    }

    /** Package-private so unit tests can inject a pre-parsed Document without network access. */
    List<ScrapeCandidate> extractCandidates(Document doc, String url) {
        var candidates = tryContainerSelectors(doc, url);
        if (candidates.isEmpty()) {
            candidates = fallbackHeadings(doc, url);
        }
        return candidates.stream()
                .filter(c -> !c.title().isBlank())
                .limit(MAX_CANDIDATES)
                .toList();
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private List<ScrapeCandidate> tryContainerSelectors(Document doc, String url) {
        for (var selector : CONTAINER_SELECTORS) {
            var elements = doc.select(selector);
            if (elements.size() >= 2) {
                return elements.stream()
                        .map(el -> toCandidate(el, url))
                        .toList();
            }
        }
        return List.of();
    }

    private List<ScrapeCandidate> fallbackHeadings(Document doc, String url) {
        return doc.select("h2, h3").stream()
                .map(h -> {
                    var title = h.text().strip();
                    var next = h.nextElementSibling();
                    var desc = (next != null && next.tagName().equals("p")) ? next.text().strip() : null;
                    return new ScrapeCandidate(title, blankToNull(desc), null, url, OffsetDateTime.now());
                })
                .toList();
    }

    private ScrapeCandidate toCandidate(Element el, String url) {
        var heading = el.selectFirst("h1, h2, h3, h4");
        var title = heading != null
                ? heading.text().strip()
                : el.text().strip().lines().findFirst().orElse("").strip();

        var para = el.selectFirst("p");
        var description = para != null ? blankToNull(para.text().strip()) : null;

        return new ScrapeCandidate(title, description, extractDateText(el), url, OffsetDateTime.now());
    }

    private String extractDateText(Element el) {
        // Prefer dedicated date elements
        var dateEl = el.selectFirst("[class*=date],[class*=Date],[class*=tid],time,time[datetime]");
        if (dateEl != null) {
            var dt = dateEl.attr("datetime");
            if (!dt.isBlank()) return dt;
            if (!dateEl.text().isBlank()) return dateEl.text().strip();
        }
        // Regex fallback over full element text
        var m = DATE_PATTERN.matcher(el.text());
        return m.find() ? m.group().strip() : null;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
