package com.umeaevents.scraping;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/scraped-events")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminScrapedEventController {

    private final ScrapedEventService service;

    @GetMapping
    public Page<ScrapedEventResponse> list(
            @RequestParam(required = false) ScrapedEventStatus status,
            Pageable pageable) {
        return service.list(status, pageable);
    }

    @GetMapping("/{id}")
    public ScrapedEventResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PostMapping("/import")
    public ResponseEntity<ScrapedEventResponse> importManual(
            @Valid @RequestBody ImportScrapedEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.importManual(request));
    }

    @PostMapping("/{id}/reject")
    public ScrapedEventResponse reject(
            @PathVariable UUID id,
            @RequestBody RejectScrapedEventRequest request,
            Principal principal) {
        return service.reject(id, request, principal.getName());
    }

    @PostMapping("/{id}/promote")
    public ScrapedEventResponse promote(
            @PathVariable UUID id,
            @Valid @RequestBody PromoteScrapedEventRequest request,
            Principal principal) {
        return service.promote(id, request, principal.getName());
    }
}
