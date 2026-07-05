package com.umeaevents.venue;

import com.umeaevents.venue.dto.VenueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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

    @PatchMapping("/{id}/owner")
    @Operation(summary = "Tilldela en lokal till en användare (ägarbyte)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public VenueResponse assignOwner(@PathVariable UUID id, @Valid @RequestBody AssignOwnerRequest request) {
        return venueService.assignOwner(id, request.ownerId());
    }

    public record AssignOwnerRequest(@NotNull UUID ownerId) {}
}
