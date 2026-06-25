package com.umeaevents.venue;

import com.umeaevents.venue.dto.CreateVenueRequest;
import com.umeaevents.venue.dto.UpdateVenueRequest;
import com.umeaevents.venue.dto.VenueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
@Tag(name = "Venues", description = "Restauranger, pubar och övriga lokaler")
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    @Operation(summary = "Lista alla aktiva venues (paginerat)")
    public Page<VenueResponse> list(Pageable pageable) {
        return venueService.listActive(pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista inloggad ägares venues", security = @SecurityRequirement(name = "bearerAuth"))
    public Page<VenueResponse> listMine(@AuthenticationPrincipal UserDetails user, Pageable pageable) {
        return venueService.listMine(user.getUsername(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Hämta en specifik venue")
    public VenueResponse getById(@PathVariable UUID id) {
        return venueService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RESTAURANT', 'ADMIN')")
    @Operation(summary = "Skapa ny venue", security = @SecurityRequirement(name = "bearerAuth"))
    public VenueResponse create(
            @Valid @RequestBody CreateVenueRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return venueService.create(request, user.getUsername());
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Uppdatera venue (ägare eller admin)", security = @SecurityRequirement(name = "bearerAuth"))
    public VenueResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVenueRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return venueService.update(id, request, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Avaktivera venue (ägare eller admin)", security = @SecurityRequirement(name = "bearerAuth"))
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        venueService.delete(id, user.getUsername());
    }
}
