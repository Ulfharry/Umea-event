package com.umeaevents.scraping;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/scraper")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminScraperController {

    private final JsoupHtmlScraper scraper;
    private final ScrapedEventService service;

    /**
     * Fetch a public URL, extract event candidates, and save them as PENDING_REVIEW.
     * Never publishes anything automatically.
     */
    @PostMapping("/test")
    public ResponseEntity<List<ScrapedEventResponse>> test(
            @Valid @RequestBody ScraperTestRequest request) {

        List<ScrapeCandidate> candidates;
        try {
            candidates = scraper.scrape(request.url());
        } catch (IOException e) {
            throw new ScrapingException("Could not fetch URL: " + e.getMessage(), e);
        }

        if (candidates.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        var saved = service.saveFromScraper(candidates);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
