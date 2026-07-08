package com.umeaevents.venue.dto;

import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueType;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Admin view of a venue — includes inactive ones and the owner's email. */
public record AdminVenueResponse(
        UUID id,
        String name,
        String description,
        VenueType type,
        String address,
        String imageUrl,
        UUID ownerId,
        String ownerEmail,
        boolean active,
        OffsetDateTime createdAt
) {
    public static AdminVenueResponse from(Venue v) {
        return new AdminVenueResponse(
                v.getId(),
                v.getName(),
                v.getDescription(),
                v.getType(),
                v.getAddress(),
                v.getImageUrl(),
                v.getOwner().getId(),
                v.getOwner().getEmail(),
                v.isActive(),
                v.getCreatedAt()
        );
    }
}
