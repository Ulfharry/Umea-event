package com.umeaevents.scraping;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin management of configured scrape sources. Adding a venue website is one POST here;
 * the weekly job then scrapes it automatically. "Run now" triggers a source on demand.
 */
@RestController
@RequestMapping("/api/v1/admin/scrape-sources")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminScrapeSourceController {

    private final ScrapeSourceService service;

    @GetMapping
    public List<ScrapeSourceResponse> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<ScrapeSourceResponse> create(@Valid @RequestBody ScrapeSourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ScrapeSourceResponse update(@PathVariable UUID id, @Valid @RequestBody ScrapeSourceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Scrape this source immediately. Stages new events as PENDING_REVIEW; never auto-publishes. */
    @PostMapping("/{id}/run")
    public ScrapeSourceResponse runNow(@PathVariable UUID id) {
        return service.runNow(id);
    }
}
