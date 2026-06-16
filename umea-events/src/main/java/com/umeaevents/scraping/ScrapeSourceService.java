package com.umeaevents.scraping;

import com.umeaevents.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
@RequiredArgsConstructor
public class ScrapeSourceService {

    private final ScrapeSourceRepository sourceRepo;
    private final SitemapScraper sitemapScraper;
    private final ScrapedEventService scrapedEventService;

    @Transactional
    public ScrapeSourceResponse create(ScrapeSourceRequest request) {
        validatePattern(request.urlPattern());
        if (sourceRepo.existsBySitemapUrlAndUrlPattern(request.sitemapUrl(), request.urlPattern())) {
            throw new IllegalStateException("A source with this sitemap URL and pattern already exists");
        }
        var source = ScrapeSource.builder()
                .name(request.name())
                .sitemapUrl(request.sitemapUrl())
                .urlPattern(request.urlPattern())
                .enabled(request.enabled() == null || request.enabled())
                .build();
        return ScrapeSourceResponse.from(sourceRepo.save(source));
    }

    @Transactional(readOnly = true)
    public List<ScrapeSourceResponse> list() {
        return sourceRepo.findAll().stream().map(ScrapeSourceResponse::from).toList();
    }

    @Transactional
    public void delete(UUID id) {
        if (!sourceRepo.existsById(id)) {
            throw new ResourceNotFoundException("Scrape source not found");
        }
        sourceRepo.deleteById(id);
    }

    /**
     * Run one source synchronously now (admin "run now"). Fetch failures become a ScrapingException
     * (HTTP 502); the last-run fields are recorded either way.
     */
    public ScrapeSourceResponse runNow(UUID id) {
        var source = sourceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scrape source not found"));
        try {
            runSource(source);
        } catch (IOException e) {
            throw new ScrapingException("Could not fetch sitemap: " + e.getMessage(), e);
        }
        return ScrapeSourceResponse.from(source);
    }

    /**
     * Scrape one source and stage any new events. Records last-run timestamp, count and error
     * (null on success). Package-private — shared by the scheduled job. Rethrows fetch failures
     * after recording them so callers can react.
     */
    int runSource(ScrapeSource source) throws IOException {
        try {
            var candidates = sitemapScraper.scrape(source.getSitemapUrl(), source.getUrlPattern());
            var saved = scrapedEventService.saveFromSitemap(candidates);
            source.setLastRunAt(OffsetDateTime.now());
            source.setLastRunNewCount(saved.size());
            source.setLastRunError(null);
            sourceRepo.save(source);
            return saved.size();
        } catch (IOException e) {
            source.setLastRunAt(OffsetDateTime.now());
            source.setLastRunError(e.getMessage());
            sourceRepo.save(source);
            throw e;
        }
    }

    private void validatePattern(String pattern) {
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid urlPattern regex: " + e.getMessage());
        }
    }
}
