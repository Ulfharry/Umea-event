package com.umeaevents.event;

import com.umeaevents.event.dto.CreateEventRequest;
import com.umeaevents.event.dto.CreateRecurringEventRequest;
import com.umeaevents.event.dto.EventOccurrenceResponse;
import com.umeaevents.event.dto.EventResponse;
import com.umeaevents.event.dto.UpdateEventRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Evenemang och föreställningar")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "Sök publicerade events (occurrence-vy, paginerat)")
    public Page<EventOccurrenceResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable) {
        return eventService.search(q, categoryId, venueId, from, to, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista inloggad ägares event (alla statusar)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public Page<EventResponse> listMine(
            @RequestParam(required = false) EventStatus status,
            @AuthenticationPrincipal UserDetails user,
            Pageable pageable) {
        return eventService.listMine(user.getUsername(), status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Hämta en specifik occurrence")
    public EventOccurrenceResponse getById(@PathVariable UUID id) {
        return eventService.getOccurrenceById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RESTAURANT', 'ADMIN')")
    @Operation(summary = "Skapa event (skapas som DRAFT)", security = @SecurityRequirement(name = "bearerAuth"))
    public EventResponse create(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.create(request, user.getUsername());
    }

    @PostMapping("/recurring")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RESTAURANT', 'ADMIN')")
    @Operation(summary = "Skapa återkommande event (skapas som DRAFT, occurrences genereras av jobb)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public EventResponse createRecurring(
            @Valid @RequestBody CreateRecurringEventRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.createRecurring(request, user.getUsername());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Skicka in event för granskning (DRAFT → PENDING_REVIEW)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public EventResponse submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.submit(id, user.getUsername());
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publicera event (PENDING_REVIEW → PUBLISHED)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public EventResponse publish(@PathVariable UUID id) {
        return eventService.publish(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Avboka event", security = @SecurityRequirement(name = "bearerAuth"))
    public EventResponse cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.cancel(id, user.getUsername());
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Redigera event (ägare eller admin)", security = @SecurityRequirement(name = "bearerAuth"))
    public EventResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return eventService.update(id, request, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ta bort event permanent (ägare eller admin)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        eventService.delete(id, user.getUsername());
    }
}
