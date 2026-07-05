package com.umeaevents.admin;

import com.umeaevents.event.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminEventResponse(
        UUID id,
        String title,
        String description,
        String imageUrl,
        UUID venueId,
        String venueName,
        UUID categoryId,
        String categoryName,
        UUID ownerId,
        String ownerEmail,
        String status,
        boolean recurring,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AdminEventResponse from(Event e, boolean recurring) {
        return new AdminEventResponse(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getImageUrl(),
                e.getVenue().getId(),
                e.getVenue().getName(),
                e.getCategory().getId(),
                e.getCategory().getName(),
                e.getOwner().getId(),
                e.getOwner().getEmail(),
                e.getStatus().name(),
                recurring,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
