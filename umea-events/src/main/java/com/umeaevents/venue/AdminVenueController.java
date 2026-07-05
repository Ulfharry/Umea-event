package com.umeaevents.venue;

import com.umeaevents.venue.dto.AdminVenueResponse;
import com.umeaevents.venue.dto.VenueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/venues")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin: Venues", description = "Administrativ hantering av lokaler")
public class AdminVenueController {

    private final VenueService venueService;

    @GetMapping
    @Operation(summary = "Lista alla lokaler inkl. inaktiva (admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public Page<AdminVenueResponse> list(Pageable pageable) {
        return venueService.listForAdmin(pageable);
    }

    @PatchMapping("/{id}/owner")
    @Operation(summary = "Tilldela en lokal till en användare (ägarbyte)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public VenueResponse assignOwner(@PathVariable UUID id, @Valid @RequestBody AssignOwnerRequest request) {
        return venueService.assignOwner(id, request.ownerId());
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Aktivera eller inaktivera en lokal",
            security = @SecurityRequirement(name = "bearerAuth"))
    public VenueResponse setActive(@PathVariable UUID id, @Valid @RequestBody SetActiveRequest request) {
        return venueService.setActive(id, request.active());
    }

    public record AssignOwnerRequest(@NotNull UUID ownerId) {}

    public record SetActiveRequest(@NotNull Boolean active) {}
}
