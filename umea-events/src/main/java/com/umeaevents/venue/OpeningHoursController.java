package com.umeaevents.venue;

import com.umeaevents.venue.dto.OpenVenueResponse;
import com.umeaevents.venue.dto.OpeningHoursDto;
import com.umeaevents.venue.dto.SetOpeningHoursRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
@Tag(name = "Öppettider", description = "Öppettider per lokal + vilka som har öppet nu")
public class OpeningHoursController {

    private final OpeningHoursService service;

    @GetMapping("/open-now")
    @Operation(summary = "Lokaler som har öppet nu (eller öppnar senare idag)")
    public List<OpenVenueResponse> openNow() {
        return service.openVenues();
    }

    @GetMapping("/{id}/opening-hours")
    @Operation(summary = "Hämta en lokals öppettider")
    public List<OpeningHoursDto> get(@PathVariable UUID id) {
        return service.getForVenue(id);
    }

    @PutMapping("/{id}/opening-hours")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sätt en lokals öppettider (ägare eller admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<OpeningHoursDto> set(
            @PathVariable UUID id,
            @Valid @RequestBody SetOpeningHoursRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return service.setForVenue(id, request, user.getUsername());
    }
}
